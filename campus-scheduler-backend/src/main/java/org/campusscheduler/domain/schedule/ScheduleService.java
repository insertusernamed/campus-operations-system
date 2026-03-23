package org.campusscheduler.domain.schedule;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.Room.AvailabilityStatus;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service layer for Schedule business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;

    /**
     * Get all schedules.
     */
    public List<Schedule> findAll() {
        return scheduleRepository.findAll().stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Find a schedule by ID.
     */
    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id)
                .map(schedule -> {
                    initialize(schedule);
                    return schedule;
                });
    }

    /**
     * Find schedules by room ID.
     */
    public List<Schedule> findByRoomId(Long roomId) {
        return scheduleRepository.findByRoomId(roomId).stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Find schedules by course ID.
     */
    public List<Schedule> findByCourseId(Long courseId) {
        return scheduleRepository.findByCourseId(courseId).stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Find schedules by instructor ID.
     */
    public List<Schedule> findByInstructorId(Long instructorId) {
        return scheduleRepository.findByCourseInstructorId(instructorId).stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Find schedules by time slot ID.
     */
    public List<Schedule> findByTimeSlotId(Long timeSlotId) {
        return scheduleRepository.findByTimeSlotId(timeSlotId).stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Find schedules by semester.
     */
    public List<Schedule> findBySemester(String semester) {
        return scheduleRepository.findBySemester(semester).stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Create a new schedule.
     *
     * @param courseId   the course ID
     * @param roomId     the room ID
     * @param timeSlotId the time slot ID
     * @param semester   the semester
     * @return the created schedule, or empty if course/room/timeslot not found
     * @throws ScheduleConflictException if room is already booked or capacity is
     *                                   insufficient
     */
    @Transactional
    public Optional<Schedule> create(Long courseId, Long roomId, Long timeSlotId, String semester) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(timeSlotId);
        if (timeSlotOpt.isEmpty()) {
            return Optional.empty();
        }

        Course course = courseOpt.get();
        Room room = roomOpt.get();
        TimeSlot timeSlot = timeSlotOpt.get();

        AvailabilityStatus availabilityStatus = room.getAvailabilityStatus() == null
                ? AvailabilityStatus.AVAILABLE
                : room.getAvailabilityStatus();
        if (availabilityStatus != AvailabilityStatus.AVAILABLE) {
            throw new ScheduleConflictException(
                    "Room " + room.getRoomNumber() + " is not available for scheduling (" +
                            availabilityStatus + ")");
        }

        // Check room capacity
        if (room.getCapacity() < course.getEnrollmentCapacity()) {
            throw new ScheduleConflictException(
                    "Room capacity (" + room.getCapacity() + ") is insufficient for course enrollment ("
                            + course.getEnrollmentCapacity() + ")");
        }

        // Check for room conflicts within the same semester
        if (hasRoomConflict(roomId, timeSlotId, semester)) {
            throw new ScheduleConflictException(
                    "Room " + room.getRoomNumber() + " is already booked for this time slot in " + semester);
        }

        Schedule schedule = Schedule.builder()
                .course(course)
                .room(room)
                .timeSlot(timeSlot)
                .semester(semester)
                .build();

        Schedule saved = scheduleRepository.save(schedule);
        initialize(saved);
        return Optional.of(saved);
    }

    /**
     * Delete a schedule by ID.
     */
    @Transactional
    public boolean delete(Long id) {
        if (scheduleRepository.existsById(id)) {
            scheduleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Update schedule room and time slot.
     */
    @Transactional
    public Optional<Schedule> updateScheduleRoomTime(Long scheduleId, Long roomId, Long timeSlotId) {
        return updateScheduleRoomTime(scheduleId, roomId, timeSlotId, null);
    }

    /**
     * Update schedule room and time slot with conflict validation.
     */
    @Transactional
    public Optional<Schedule> updateScheduleRoomTime(
            Long scheduleId,
            Long roomId,
            Long timeSlotId,
            java.util.function.Supplier<List<String>> conflictValidator) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(timeSlotId);
        if (timeSlotOpt.isEmpty()) {
            return Optional.empty();
        }

        if (conflictValidator != null) {
            List<String> conflicts = conflictValidator.get();
            if (!conflicts.isEmpty()) {
                throw new ScheduleConflictException(conflicts.get(0));
            }
        }

        Schedule schedule = scheduleOpt.get();
        schedule.setRoom(roomOpt.get());
        schedule.setTimeSlot(timeSlotOpt.get());

        Schedule saved = scheduleRepository.save(schedule);
        initialize(saved);
        return Optional.of(saved);
    }

    /**
     * Check if a room has a conflict for a given time slot (any semester).
     */
    public boolean hasRoomConflict(Long roomId, Long timeSlotId) {
        List<Schedule> existing = scheduleRepository.findByRoomIdAndTimeSlotId(roomId, timeSlotId);
        return !existing.isEmpty();
    }

    /**
     * Check if a room has a conflict for a given time slot and semester.
     */
    public boolean hasRoomConflict(Long roomId, Long timeSlotId, String semester) {
        List<Schedule> existing = scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(
                roomId, timeSlotId, semester);
        return !existing.isEmpty();
    }

    private void initialize(Schedule schedule) {
        if (schedule.getCourse() != null) {
            schedule.getCourse().getId();
            schedule.getCourse().getCode();
            if (schedule.getCourse().getInstructor() != null) {
                schedule.getCourse().getInstructor().getId();
                schedule.getCourse().getInstructor().getFirstName();
            }
        }
        if (schedule.getRoom() != null) {
            schedule.getRoom().getId();
            schedule.getRoom().getRoomNumber();
            schedule.getRoom().getFeatureSet().size();
            schedule.getRoom().getAccessibilityFlags().size();
        }

        TimeSlot timeSlot = schedule.getTimeSlot();
        if (timeSlot != null) {
            timeSlot.getId();
            timeSlot.getDayOfWeek();
            timeSlot.getStartTime();
            timeSlot.getEndTime();
        }
    }
}
