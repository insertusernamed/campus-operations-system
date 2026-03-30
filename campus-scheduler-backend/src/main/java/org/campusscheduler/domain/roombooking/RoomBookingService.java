package org.campusscheduler.domain.roombooking;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.Room.AvailabilityStatus;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.room.RoomResponse;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.schedule.ScheduleResponse;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.student.StudentRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Business rules and privacy policy for student room bookings.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomBookingService {

    private static final int MAX_BOOKINGS_PER_DAY = 2;

    private final RoomBookingRepository roomBookingRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;

    public List<RoomBookingResponse> findVisibleBookings(
            String semester,
            Long studentId,
            String viewerRole,
            Long viewerStudentId) {
        ViewerContext viewerContext = ViewerContext.of(viewerRole, viewerStudentId);
        return roomBookingRepository.findByFilters(semester, studentId).stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .sorted(roomBookingComparator())
                .map(booking -> toResponse(booking, viewerContext))
                .toList();
    }

    @Transactional
    public Optional<RoomBookingResponse> create(
            CreateRoomBookingRequest request,
            String viewerRole,
            Long viewerStudentId) {
        Optional<Student> bookedByOpt = studentRepository.findById(request.getStudentId());
        if (bookedByOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Room> roomOpt = roomRepository.findById(request.getRoomId());
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(request.getTimeSlotId());
        if (timeSlotOpt.isEmpty()) {
            return Optional.empty();
        }

        Student bookedBy = bookedByOpt.get();
        Room room = roomOpt.get();
        TimeSlot timeSlot = timeSlotOpt.get();
        Set<Student> participants = resolveParticipants(request.getParticipantEmails(), bookedBy);

        validateRoom(room, timeSlot, request.getSemester());
        validateStudentLimits(bookedBy, participants, timeSlot, request.getSemester());

        RoomBooking booking = RoomBooking.builder()
                .room(room)
                .timeSlot(timeSlot)
                .semester(request.getSemester())
                .bookedBy(bookedBy)
                .participants(new LinkedHashSet<>(participants))
                .build();

        RoomBooking saved = roomBookingRepository.save(booking);
        initialize(saved);
        ViewerContext viewerContext = ViewerContext.of(viewerRole, viewerStudentId);
        return Optional.of(toResponse(saved, viewerContext));
    }

    public List<RoomBookingStudentLookupResponse> searchStudentsByEmail(
            String query,
            String semester,
            Long timeSlotId,
            List<Long> excludeStudentIds) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found"));

        Set<Long> excluded = new LinkedHashSet<>(excludeStudentIds == null ? List.of() : excludeStudentIds);

        return studentRepository.findTop8ByEmailContainingIgnoreCaseOrderByEmailAsc(query.trim()).stream()
                .filter(Objects::nonNull)
                .filter(student -> !excluded.contains(student.getId()))
                .map(student -> new RoomBookingStudentLookupResponse(
                        student.getId(),
                        student.getEmail(),
                        formatStudentName(student),
                        hasClassDuringPeriod(student.getId(), semester, timeSlot)))
                .toList();
    }

    private Set<Student> resolveParticipants(List<String> participantEmails, Student bookedBy) {
        Map<Long, Student> participantsById = new LinkedHashMap<>();
        if (participantEmails == null || participantEmails.isEmpty()) {
            return Set.of();
        }

        for (String rawEmail : participantEmails) {
            String email = normalizeEmail(rawEmail);
            if (email.isEmpty()) {
                continue;
            }
            if (email.equals(normalizeEmail(bookedBy.getEmail()))) {
                continue;
            }

            Student student = studentRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Student email not found: " + email));
            participantsById.put(student.getId(), student);
        }

        return new LinkedHashSet<>(participantsById.values());
    }

    private void validateRoom(Room room, TimeSlot timeSlot, String semester) {
        AvailabilityStatus availabilityStatus = room.getAvailabilityStatus() == null
                ? AvailabilityStatus.AVAILABLE
                : room.getAvailabilityStatus();
        if (availabilityStatus != AvailabilityStatus.AVAILABLE) {
            throw new RoomBookingConflictException(
                    "Room " + room.getRoomNumber() + " is not available for student bookings (" + availabilityStatus + ")");
        }

        if (!scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(room.getId(), timeSlot.getId(), semester).isEmpty()) {
            throw new RoomBookingConflictException(
                    "Room " + room.getRoomNumber() + " is already scheduled for classes during this time slot in " + semester);
        }

        if (!roomBookingRepository.findByRoomIdAndTimeSlotIdAndSemester(room.getId(), timeSlot.getId(), semester).isEmpty()) {
            throw new RoomBookingConflictException(
                    "Room " + room.getRoomNumber() + " is already booked by a student during this time slot in " + semester);
        }
    }

    private void validateStudentLimits(Student bookedBy, Set<Student> participants, TimeSlot timeSlot, String semester) {
        List<Student> involvedStudents = new ArrayList<>();
        involvedStudents.add(bookedBy);
        involvedStudents.addAll(participants);

        for (Student student : involvedStudents) {
            if (student.getId() == null) {
                continue;
            }

            if (roomBookingRepository.existsForStudentAtTime(student.getId(), timeSlot.getId(), semester)) {
                throw new RoomBookingConflictException(
                        formatStudentName(student) + " already has a room booking during this time slot");
            }

            long dailyCount = roomBookingRepository.countForStudentOnDay(student.getId(), timeSlot.getDayOfWeek(), semester);
            if (dailyCount >= MAX_BOOKINGS_PER_DAY) {
                throw new RoomBookingConflictException(
                        formatStudentName(student) + " already has the maximum of " + MAX_BOOKINGS_PER_DAY
                                + " room bookings on " + formatDay(timeSlot.getDayOfWeek()));
            }
        }
    }

    private boolean hasClassDuringPeriod(Long studentId, String semester, TimeSlot timeSlot) {
        return enrollmentRepository.findByStudentIdAndSemester(studentId, semester).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .map(Enrollment::getSchedule)
                .filter(Objects::nonNull)
                .map(org.campusscheduler.domain.schedule.Schedule::getTimeSlot)
                .filter(Objects::nonNull)
                .anyMatch(existing -> Objects.equals(existing.getId(), timeSlot.getId()));
    }

    private RoomBookingResponse toResponse(RoomBooking booking, ViewerContext viewerContext) {
        boolean viewerIsOwner = viewerContext.studentId() != null
                && booking.getBookedBy() != null
                && Objects.equals(booking.getBookedBy().getId(), viewerContext.studentId());
        boolean viewerIsParticipant = viewerIsOwner
                || booking.getParticipants().stream()
                        .map(Student::getId)
                        .filter(Objects::nonNull)
                        .anyMatch(id -> Objects.equals(id, viewerContext.studentId()));
        boolean viewerCanSeeStudentDetails = viewerContext.isAdmin() || viewerIsOwner;

        List<RoomBookingParticipantResponse> visibleParticipants = viewerCanSeeStudentDetails
                ? booking.getParticipants().stream()
                        .sorted(Comparator.comparing(RoomBookingService::formatStudentName))
                        .map(RoomBookingParticipantResponse::from)
                        .toList()
                : List.of();

        return new RoomBookingResponse(
                booking.getId(),
                RoomResponse.fromEntity(booking.getRoom()),
                ScheduleResponse.TimeSlotSummary.from(booking.getTimeSlot()),
                booking.getSemester(),
                booking.getCreatedAt(),
                1 + booking.getParticipants().size(),
                viewerCanSeeStudentDetails,
                viewerIsOwner,
                viewerIsParticipant,
                viewerCanSeeStudentDetails ? RoomBookingParticipantResponse.from(booking.getBookedBy()) : null,
                visibleParticipants);
    }

    private void initialize(RoomBooking booking) {
        if (booking.getRoom() != null) {
            booking.getRoom().getId();
            booking.getRoom().getRoomNumber();
            booking.getRoom().getFeatureSet().size();
            booking.getRoom().getAccessibilityFlags().size();
            if (booking.getRoom().getBuilding() != null) {
                booking.getRoom().getBuilding().getId();
            }
        }
        if (booking.getTimeSlot() != null) {
            booking.getTimeSlot().getId();
            booking.getTimeSlot().getDayOfWeek();
            booking.getTimeSlot().getStartTime();
            booking.getTimeSlot().getEndTime();
        }
        if (booking.getBookedBy() != null) {
            booking.getBookedBy().getId();
            booking.getBookedBy().getFirstName();
            booking.getBookedBy().getLastName();
            booking.getBookedBy().getEmail();
        }
        booking.getParticipants().forEach(participant -> {
            participant.getId();
            participant.getFirstName();
            participant.getLastName();
            participant.getEmail();
        });
    }

    private Comparator<RoomBooking> roomBookingComparator() {
        return Comparator
                .comparing(RoomBooking::getSemester, Comparator.nullsLast(String::compareTo))
                .thenComparing(booking -> booking.getTimeSlot() != null && booking.getTimeSlot().getDayOfWeek() != null
                        ? booking.getTimeSlot().getDayOfWeek().getValue()
                        : Integer.MAX_VALUE)
                .thenComparing(booking -> booking.getTimeSlot() != null && booking.getTimeSlot().getStartTime() != null
                        ? booking.getTimeSlot().getStartTime()
                        : java.time.LocalTime.MAX)
                .thenComparing(booking -> booking.getRoom() != null && booking.getRoom().getBuildingCode() != null
                        ? booking.getRoom().getBuildingCode()
                        : "")
                .thenComparing(booking -> booking.getRoom() != null && booking.getRoom().getRoomNumber() != null
                        ? booking.getRoom().getRoomNumber()
                        : "")
                .thenComparing(booking -> booking.getId() == null ? Long.MAX_VALUE : booking.getId());
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            return "";
        }
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private static String formatDay(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return "that day";
        }
        String raw = dayOfWeek.name().toLowerCase(Locale.ROOT);
        return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
    }

    private static String formatStudentName(Student student) {
        if (student == null) {
            return "Student";
        }
        String firstName = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String lastName = student.getLastName() == null ? "" : student.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "Student" : fullName;
    }

    private record ViewerContext(boolean isAdmin, Long studentId) {

        static ViewerContext of(String viewerRole, Long viewerStudentId) {
            boolean admin = viewerRole != null && "admin".equalsIgnoreCase(viewerRole.trim());
            return new ViewerContext(admin, viewerStudentId);
        }
    }
}
