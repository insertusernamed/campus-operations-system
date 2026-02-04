package org.campusscheduler.domain.changerequest;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.schedule.ScheduleService;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service layer for schedule change request business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleChangeRequestService {

    private static final long BACK_TO_BACK_MINUTES = 15;

    private final ScheduleChangeRequestRepository changeRequestRepository;
    private final ScheduleRepository scheduleRepository;
    private final InstructorRepository instructorRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ScheduleService scheduleService;

    public List<ScheduleChangeRequest> findAll(
            ChangeRequestStatus status,
            Long instructorId,
            String semester,
            Long scheduleId) {
        return changeRequestRepository.findByFilters(status, instructorId, semester, scheduleId);
    }

    public Optional<ScheduleChangeRequest> findById(Long id) {
        return changeRequestRepository.findById(id);
    }

    @Transactional
    public Optional<ScheduleChangeRequest> create(ChangeRequestCreateRequest request) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(request.getScheduleId());
        if (scheduleOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Instructor> instructorOpt = instructorRepository.findById(request.getRequestedByInstructorId());
        if (instructorOpt.isEmpty()) {
            return Optional.empty();
        }

        if (request.getProposedRoomId() == null && request.getProposedTimeSlotId() == null) {
            throw new ChangeRequestStateException("At least one proposed change is required");
        }

        Schedule schedule = scheduleOpt.get();
        Room proposedRoom = resolveRoom(request.getProposedRoomId());
        if (request.getProposedRoomId() != null && proposedRoom == null) {
            return Optional.empty();
        }

        TimeSlot proposedTimeSlot = resolveTimeSlot(request.getProposedTimeSlotId());
        if (request.getProposedTimeSlotId() != null && proposedTimeSlot == null) {
            return Optional.empty();
        }

        ScheduleChangeRequest changeRequest = ScheduleChangeRequest.builder()
                .schedule(schedule)
                .requestedByInstructor(instructorOpt.get())
                .requestedByRole(request.getRequestedByRole())
                .status(ChangeRequestStatus.PENDING)
                .reasonCategory(request.getReasonCategory())
                .reasonDetails(request.getReasonDetails())
                .proposedRoom(proposedRoom)
                .proposedTimeSlot(proposedTimeSlot)
                .originalRoomId(schedule.getRoom().getId())
                .originalTimeSlotId(schedule.getTimeSlot().getId())
                .originalSemester(schedule.getSemester())
                .createdAt(LocalDateTime.now())
                .build();

        return Optional.of(changeRequestRepository.save(changeRequest));
    }

    @Transactional
    public Optional<ScheduleChangeRequest> approve(Long id, ChangeRequestDecisionRequest decisionRequest) {
        Optional<ScheduleChangeRequest> changeRequestOpt = changeRequestRepository.findById(id);
        if (changeRequestOpt.isEmpty()) {
            return Optional.empty();
        }

        ScheduleChangeRequest changeRequest = changeRequestOpt.get();
        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new ChangeRequestStateException("Change request has already been resolved");
        }

        Room overrideRoom = resolveRoom(decisionRequest.getProposedRoomId());
        if (decisionRequest.getProposedRoomId() != null && overrideRoom == null) {
            return Optional.empty();
        }

        TimeSlot overrideTimeSlot = resolveTimeSlot(decisionRequest.getProposedTimeSlotId());
        if (decisionRequest.getProposedTimeSlotId() != null && overrideTimeSlot == null) {
            return Optional.empty();
        }

        if (overrideRoom != null) {
            changeRequest.setProposedRoom(overrideRoom);
        }
        if (overrideTimeSlot != null) {
            changeRequest.setProposedTimeSlot(overrideTimeSlot);
        }

        Schedule schedule = changeRequest.getSchedule();
        if (changeRequest.getProposedRoom() == null && changeRequest.getProposedTimeSlot() == null) {
            throw new ChangeRequestStateException("At least one proposed change is required");
        }
        Room targetRoom = changeRequest.getProposedRoom() != null ? changeRequest.getProposedRoom() : schedule.getRoom();
        TimeSlot targetTimeSlot = changeRequest.getProposedTimeSlot() != null ? changeRequest.getProposedTimeSlot() : schedule.getTimeSlot();

        ChangeRequestValidationResponse validation = validateChange(schedule, targetRoom, targetTimeSlot);
        if (!validation.getHardConflicts().isEmpty()) {
            throw new ChangeRequestConflictException("Change request conflicts with existing schedules", validation.getHardConflicts());
        }

        try {
            if (scheduleService.updateScheduleRoomTime(
                    schedule.getId(),
                    targetRoom.getId(),
                    targetTimeSlot.getId(),
                    () -> validateChange(schedule, targetRoom, targetTimeSlot).getHardConflicts()).isEmpty()) {
                return Optional.empty();
            }
        } catch (org.campusscheduler.domain.schedule.ScheduleConflictException ex) {
            throw new ChangeRequestConflictException(ex.getMessage(), List.of(ex.getMessage()));
        }

        LocalDateTime now = LocalDateTime.now();
        changeRequest.setStatus(ChangeRequestStatus.APPROVED);
        changeRequest.setDecisionNote(decisionRequest.getDecisionNote());
        changeRequest.setReviewedAt(now);
        changeRequest.setAppliedAt(now);

        return Optional.of(changeRequestRepository.save(changeRequest));
    }

    @Transactional
    public Optional<ScheduleChangeRequest> reject(Long id, ChangeRequestDecisionRequest decisionRequest) {
        Optional<ScheduleChangeRequest> changeRequestOpt = changeRequestRepository.findById(id);
        if (changeRequestOpt.isEmpty()) {
            return Optional.empty();
        }

        ScheduleChangeRequest changeRequest = changeRequestOpt.get();
        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new ChangeRequestStateException("Change request has already been resolved");
        }

        changeRequest.setStatus(ChangeRequestStatus.REJECTED);
        changeRequest.setDecisionNote(decisionRequest.getDecisionNote());
        changeRequest.setReviewedAt(LocalDateTime.now());

        return Optional.of(changeRequestRepository.save(changeRequest));
    }

    public Optional<ChangeRequestValidationResponse> validate(ChangeRequestValidationRequest request) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(request.getScheduleId());
        if (scheduleOpt.isEmpty()) {
            return Optional.empty();
        }

        if (request.getProposedRoomId() == null && request.getProposedTimeSlotId() == null) {
            throw new ChangeRequestStateException("At least one proposed change is required");
        }

        Schedule schedule = scheduleOpt.get();
        Room targetRoom = resolveRoom(request.getProposedRoomId());
        if (request.getProposedRoomId() != null && targetRoom == null) {
            return Optional.empty();
        }

        TimeSlot targetTimeSlot = resolveTimeSlot(request.getProposedTimeSlotId());
        if (request.getProposedTimeSlotId() != null && targetTimeSlot == null) {
            return Optional.empty();
        }

        if (targetRoom == null) {
            targetRoom = schedule.getRoom();
        }

        if (targetTimeSlot == null) {
            targetTimeSlot = schedule.getTimeSlot();
        }

        return Optional.of(validateChange(schedule, targetRoom, targetTimeSlot));
    }

    private Room resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId).orElse(null);
    }

    private TimeSlot resolveTimeSlot(Long timeSlotId) {
        if (timeSlotId == null) {
            return null;
        }
        return timeSlotRepository.findById(timeSlotId).orElse(null);
    }

    private ChangeRequestValidationResponse validateChange(Schedule schedule, Room room, TimeSlot timeSlot) {
        List<String> hardConflicts = new ArrayList<>();
        List<String> softWarnings = new ArrayList<>();

        if (room == null || timeSlot == null) {
            hardConflicts.add("Room and time slot are required for validation");
            return ChangeRequestValidationResponse.of(hardConflicts, softWarnings);
        }

        Course course = schedule.getCourse();
        if (course == null) {
            hardConflicts.add("Course information is required for validation");
            return ChangeRequestValidationResponse.of(hardConflicts, softWarnings);
        }

        Integer enrollmentCapacity = course.getEnrollmentCapacity();
        if (enrollmentCapacity == null) {
            softWarnings.add("Course enrollment capacity is not specified; skipping room capacity validation");
        } else if (room.getCapacity() < enrollmentCapacity) {
            hardConflicts.add("Room capacity (" + room.getCapacity() + ") is insufficient for enrollment ("
                    + enrollmentCapacity + ")");
        }

        for (Schedule existing : scheduleRepository.findByRoomIdAndSemester(room.getId(), schedule.getSemester())) {
            if (existing.getId().equals(schedule.getId())) {
                continue;
            }
            TimeSlot existingTimeSlot = existing.getTimeSlot();
            if (existingTimeSlot == null || existingTimeSlot.getDayOfWeek() == null) {
                continue;
            }
            if (!existingTimeSlot.getDayOfWeek().equals(timeSlot.getDayOfWeek())) {
                continue;
            }
            if (existingTimeSlot.overlapsWith(timeSlot)) {
                String courseCode = existing.getCourse() != null ? existing.getCourse().getCode() : "another course";
                hardConflicts.add("Room conflict with " + courseCode + " at this time");
                break;
            }
        }

        if (course.getInstructor() != null) {
            Long instructorId = course.getInstructor().getId();
            List<Schedule> instructorSchedules = scheduleRepository
                    .findByCourseInstructorIdAndSemesterAndTimeSlotDayOfWeek(
                            instructorId,
                            schedule.getSemester(),
                            timeSlot.getDayOfWeek());

            for (Schedule existing : instructorSchedules) {
                if (existing.getId().equals(schedule.getId())) {
                    continue;
                }
                TimeSlot existingTimeSlot = existing.getTimeSlot();
                if (existingTimeSlot == null) {
                    continue;
                }
                if (existingTimeSlot.overlapsWith(timeSlot)) {
                    String courseCode = existing.getCourse() != null ? existing.getCourse().getCode() : "another course";
                    hardConflicts.add("Instructor conflict with " + courseCode + " at this time");
                    break;
                }
            }

            softWarnings.addAll(buildTravelWarnings(course, instructorId, room, timeSlot, schedule));
        }

        String mismatchMessage = roomTypeMismatchMessage(course, room);
        if (mismatchMessage != null) {
            softWarnings.add(mismatchMessage);
        }

        return ChangeRequestValidationResponse.of(hardConflicts, softWarnings);
    }

    private List<String> buildTravelWarnings(
            Course course,
            Long instructorId,
            Room targetRoom,
            TimeSlot targetTimeSlot,
            Schedule schedule) {
        List<String> warnings = new ArrayList<>();
        List<Schedule> sameDaySchedules = scheduleRepository
                .findByCourseInstructorIdAndSemesterAndTimeSlotDayOfWeek(
                        instructorId,
                        schedule.getSemester(),
                        targetTimeSlot.getDayOfWeek());

        for (Schedule other : sameDaySchedules) {
            if (other.getId().equals(schedule.getId())) {
                continue;
            }
            TimeSlot otherTimeSlot = other.getTimeSlot();
            Room otherRoom = other.getRoom();
            if (otherTimeSlot == null || otherRoom == null) {
                continue;
            }
            if (!otherTimeSlot.getDayOfWeek().equals(targetTimeSlot.getDayOfWeek())) {
                continue;
            }
            if (!isBackToBack(otherTimeSlot, targetTimeSlot)) {
                continue;
            }
            if (Objects.equals(otherRoom.getBuildingId(), targetRoom.getBuildingId())) {
                continue;
            }
            String otherCourse = other.getCourse() != null ? other.getCourse().getCode() : "another course";
            String targetCourse = course != null ? course.getCode() : "this course";
            String targetBuilding = targetRoom.getBuildingCode() != null ? targetRoom.getBuildingCode() : "another building";
            String otherBuilding = otherRoom.getBuildingCode() != null ? otherRoom.getBuildingCode() : "another building";
            warnings.add("Back-to-back classes in different buildings within " + BACK_TO_BACK_MINUTES
                    + " minutes: " + targetCourse + " and " + otherCourse + " (" + otherBuilding + " -> "
                    + targetBuilding + ")");
        }

        return warnings;
    }

    private boolean isBackToBack(TimeSlot a, TimeSlot b) {
        return isWithinMinutes(a.getEndTime(), b.getStartTime())
                || isWithinMinutes(b.getEndTime(), a.getStartTime());
    }

    private boolean isWithinMinutes(java.time.LocalTime firstEnd, java.time.LocalTime secondStart) {
        if (secondStart.isBefore(firstEnd)) {
            return false;
        }
        long minutes = Duration.between(firstEnd, secondStart).toMinutes();
        return minutes <= BACK_TO_BACK_MINUTES;
    }

    private String roomTypeMismatchMessage(Course course, Room room) {
        if (course == null || room == null) {
            return null;
        }

        String department = course.getDepartment();
        String roomType = room.getType().name();

        if (department != null && (department.contains("Chemistry")
                || department.contains("Biology")
                || department.contains("Physics"))) {
            if (!"LAB".equals(roomType)) {
                return "Room type mismatch: science courses should be scheduled in a LAB";
            }
        }

        if (course.getEnrollmentCapacity() != null && course.getEnrollmentCapacity() > 80) {
            if (!"LECTURE_HALL".equals(roomType)) {
                return "Room type mismatch: large courses should be scheduled in a LECTURE_HALL";
            }
        }

        return null;
    }
}
