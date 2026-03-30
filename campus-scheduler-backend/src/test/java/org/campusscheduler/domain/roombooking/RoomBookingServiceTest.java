package org.campusscheduler.domain.roombooking;

import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.student.StudentRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomBookingServiceTest {

    @Mock
    private RoomBookingRepository roomBookingRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private org.campusscheduler.domain.room.RoomRepository roomRepository;

    @Mock
    private org.campusscheduler.domain.timeslot.TimeSlotRepository timeSlotRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private RoomBookingService roomBookingService;

    private Student owner;
    private Student collaborator;
    private Student thirdStudent;
    private Room room;
    private TimeSlot timeSlot;
    private RoomBooking booking;
    private String bookingSemester;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        owner = Student.builder()
                .id(1L)
                .studentNumber("S100001")
                .firstName("Maya")
                .lastName("Patel")
                .email("maya.patel@students.campus.edu")
                .department("Computer Science")
                .yearLevel(3)
                .targetCourseLoad(4)
                .build();

        collaborator = Student.builder()
                .id(2L)
                .studentNumber("S100002")
                .firstName("Jonah")
                .lastName("Lee")
                .email("jonah.lee@students.campus.edu")
                .department("Computer Science")
                .yearLevel(2)
                .targetCourseLoad(4)
                .build();

        thirdStudent = Student.builder()
                .id(3L)
                .studentNumber("S100003")
                .firstName("Ava")
                .lastName("Garcia")
                .email("ava.garcia@students.campus.edu")
                .department("Biology")
                .yearLevel(1)
                .targetCourseLoad(4)
                .build();

        room = Room.builder()
                .id(10L)
                .roomNumber("201")
                .capacity(24)
                .type(Room.RoomType.SEMINAR)
                .availabilityStatus(Room.AvailabilityStatus.AVAILABLE)
                .build();

        timeSlot = TimeSlot.builder()
                .id(20L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 15))
                .label("Mon 10:00")
                .build();

        bookingDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        bookingSemester = computeSemesterLabel(bookingDate);

        booking = RoomBooking.builder()
                .id(50L)
                .room(room)
                .timeSlot(timeSlot)
                .semester(bookingSemester)
                .bookingDate(bookingDate)
                .bookedBy(owner)
                .participants(new LinkedHashSet<>(Set.of(collaborator)))
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create booking and expose owner details to the booking owner")
        void shouldCreateBooking() {
            CreateRoomBookingRequest request = new CreateRoomBookingRequest();
            request.setStudentId(1L);
            request.setRoomId(10L);
            request.setTimeSlotId(20L);
            request.setSemester(bookingSemester);
            request.setBookingDate(bookingDate);
            request.setParticipantEmails(List.of("jonah.lee@students.campus.edu"));

            when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(timeSlot));
            when(studentRepository.findByEmail("jonah.lee@students.campus.edu")).thenReturn(Optional.of(collaborator));
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(10L, 20L, bookingSemester)).thenReturn(List.of());
            when(roomBookingRepository.findConflictingRoomBookings(10L, 20L, bookingDate, bookingSemester)).thenReturn(List.of());
            when(roomBookingRepository.existsForStudentAtTime(1L, 20L, bookingDate, bookingSemester)).thenReturn(false);
            when(roomBookingRepository.existsForStudentAtTime(2L, 20L, bookingDate, bookingSemester)).thenReturn(false);
            when(roomBookingRepository.countForStudentOnDate(1L, bookingDate, bookingSemester, DayOfWeek.MONDAY)).thenReturn(1L);
            when(roomBookingRepository.countForStudentOnDate(2L, bookingDate, bookingSemester, DayOfWeek.MONDAY)).thenReturn(0L);
            when(roomBookingRepository.save(any(RoomBooking.class))).thenReturn(booking);

            Optional<RoomBookingResponse> response = roomBookingService.create(request, "student", 1L);

            assertThat(response).isPresent();
            assertThat(response.get().viewerCanSeeStudentDetails()).isTrue();
            assertThat(response.get().viewerIsOwner()).isTrue();
            assertThat(response.get().participants()).hasSize(1);
            assertThat(response.get().bookedBy()).isNotNull();
            verify(roomBookingRepository).save(any(RoomBooking.class));
        }

        @Test
        @DisplayName("should reject booking when room is already scheduled for a class")
        void shouldRejectScheduledRoom() {
            CreateRoomBookingRequest request = new CreateRoomBookingRequest();
            request.setStudentId(1L);
            request.setRoomId(10L);
            request.setTimeSlotId(20L);
            request.setSemester(bookingSemester);
            request.setBookingDate(bookingDate);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(timeSlot));
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(10L, 20L, bookingSemester))
                    .thenReturn(List.of(Schedule.builder().id(99L).build()));

            assertThatThrownBy(() -> roomBookingService.create(request, "student", 1L))
                    .isInstanceOf(RoomBookingConflictException.class)
                    .hasMessageContaining("scheduled for classes");
        }

        @Test
        @DisplayName("should reject booking when a participant already has a booking in the same slot")
        void shouldRejectParticipantTimeConflict() {
            CreateRoomBookingRequest request = new CreateRoomBookingRequest();
            request.setStudentId(1L);
            request.setRoomId(10L);
            request.setTimeSlotId(20L);
            request.setSemester(bookingSemester);
            request.setBookingDate(bookingDate);
            request.setParticipantEmails(List.of("jonah.lee@students.campus.edu"));

            when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(timeSlot));
            when(studentRepository.findByEmail("jonah.lee@students.campus.edu")).thenReturn(Optional.of(collaborator));
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(10L, 20L, bookingSemester)).thenReturn(List.of());
            when(roomBookingRepository.findConflictingRoomBookings(10L, 20L, bookingDate, bookingSemester)).thenReturn(List.of());
            when(roomBookingRepository.existsForStudentAtTime(1L, 20L, bookingDate, bookingSemester)).thenReturn(false);
            when(roomBookingRepository.existsForStudentAtTime(2L, 20L, bookingDate, bookingSemester)).thenReturn(true);
            when(roomBookingRepository.countForStudentOnDate(1L, bookingDate, bookingSemester, DayOfWeek.MONDAY)).thenReturn(0L);

            assertThatThrownBy(() -> roomBookingService.create(request, "student", 1L))
                    .isInstanceOf(RoomBookingConflictException.class)
                    .hasMessageContaining("Jonah Lee already has a room booking during this time slot");
        }

        @Test
        @DisplayName("should reject booking when the owner already has two bookings that day")
        void shouldRejectDailyLimit() {
            CreateRoomBookingRequest request = new CreateRoomBookingRequest();
            request.setStudentId(1L);
            request.setRoomId(10L);
            request.setTimeSlotId(20L);
            request.setSemester(bookingSemester);
            request.setBookingDate(bookingDate);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(timeSlot));
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(10L, 20L, bookingSemester)).thenReturn(List.of());
            when(roomBookingRepository.findConflictingRoomBookings(10L, 20L, bookingDate, bookingSemester)).thenReturn(List.of());
            when(roomBookingRepository.existsForStudentAtTime(1L, 20L, bookingDate, bookingSemester)).thenReturn(false);
            when(roomBookingRepository.countForStudentOnDate(1L, bookingDate, bookingSemester, DayOfWeek.MONDAY)).thenReturn(2L);

            assertThatThrownBy(() -> roomBookingService.create(request, "student", 1L))
                    .isInstanceOf(RoomBookingConflictException.class)
                    .hasMessageContaining("maximum of 2 room bookings");
        }
    }

    @Nested
    @DisplayName("findVisibleBookings")
    class FindVisibleBookings {

        @Test
        @DisplayName("should redact student details for non-admin viewers")
        void shouldRedactForNonAdmin() {
            when(roomBookingRepository.findByFilters(bookingSemester, null)).thenReturn(List.of(booking));

            List<RoomBookingResponse> result = roomBookingService.findVisibleBookings(bookingSemester, null, "instructor", null);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().viewerCanSeeStudentDetails()).isFalse();
            assertThat(result.getFirst().bookedBy()).isNull();
            assertThat(result.getFirst().participants()).isEmpty();
        }

        @Test
        @DisplayName("should expose student details to admins")
        void shouldExposeToAdmins() {
            when(roomBookingRepository.findByFilters(bookingSemester, null)).thenReturn(List.of(booking));

            List<RoomBookingResponse> result = roomBookingService.findVisibleBookings(bookingSemester, null, "admin", null);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().viewerCanSeeStudentDetails()).isTrue();
            assertThat(result.getFirst().bookedBy()).isNotNull();
            assertThat(result.getFirst().participants()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("searchStudentsByEmail")
    class SearchStudentsByEmail {

        @Test
        @DisplayName("should return only basic info plus class overlap state")
        void shouldReturnLookupInfo() {
            Enrollment overlappingEnrollment = Enrollment.builder()
                    .id(300L)
                    .student(thirdStudent)
                    .schedule(Schedule.builder()
                            .id(400L)
                            .timeSlot(timeSlot)
                            .semester(bookingSemester)
                            .build())
                    .status(EnrollmentStatus.ENROLLED)
                    .semester(bookingSemester)
                    .build();

            when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(timeSlot));
            when(studentRepository.findTop8ByEmailContainingIgnoreCaseOrderByEmailAsc("ava"))
                    .thenReturn(List.of(thirdStudent));
            when(enrollmentRepository.findByStudentIdAndSemester(3L, bookingSemester))
                    .thenReturn(List.of(overlappingEnrollment));

            List<RoomBookingStudentLookupResponse> result =
                    roomBookingService.searchStudentsByEmail("ava", bookingSemester, 20L, List.of());

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().fullName()).isEqualTo("Ava Garcia");
            assertThat(result.getFirst().email()).isEqualTo("ava.garcia@students.campus.edu");
            assertThat(result.getFirst().hasClassDuringPeriod()).isTrue();
        }
    }

    private static String computeSemesterLabel(LocalDate date) {
        int month = date.getMonthValue();
        String term;
        if (month >= 1 && month <= 5) {
            term = "Spring";
        } else if (month >= 6 && month <= 8) {
            term = "Summer";
        } else {
            term = "Fall";
        }
        return term + " " + date.getYear();
    }
}
