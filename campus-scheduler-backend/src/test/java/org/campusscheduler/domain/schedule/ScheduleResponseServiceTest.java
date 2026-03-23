package org.campusscheduler.domain.schedule;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleResponseServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ScheduleResponseService scheduleResponseService;

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .id(10L)
                .code("CS410")
                .name("Distributed Systems")
                .credits(3)
                .enrollmentCapacity(40)
                .department("Computer Science")
                .build();

        Room room = Room.builder()
                .id(5L)
                .roomNumber("201")
                .capacity(32)
                .type(Room.RoomType.CLASSROOM)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(7L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Mon 9:00")
                .build();

        schedule = Schedule.builder()
                .id(200L)
                .course(course)
                .room(room)
                .timeSlot(timeSlot)
                .semester("Fall 2026")
                .build();
    }

    @Test
    @DisplayName("should include filled seats, waitlist, and resolved seat limit")
    void shouldIncludeFilledSeatsWaitlistAndResolvedSeatLimit() {
        Student student = Student.builder()
                .id(1L)
                .studentNumber("S300001")
                .firstName("Harper")
                .lastName("Chen")
                .email("harper.chen@student.university.edu")
                .department("Computer Science")
                .yearLevel(4)
                .build();

        when(enrollmentRepository.findByScheduleIdIn(List.of(200L))).thenReturn(List.of(
                Enrollment.builder()
                        .id(100L)
                        .student(student)
                        .course(schedule.getCourse())
                        .schedule(schedule)
                        .semester("Fall 2026")
                        .status(EnrollmentStatus.ENROLLED)
                        .build(),
                Enrollment.builder()
                        .id(101L)
                        .student(student)
                        .course(schedule.getCourse())
                        .schedule(schedule)
                        .semester("Fall 2026")
                        .status(EnrollmentStatus.WAITLISTED)
                        .build()));

        ScheduleResponse response = scheduleResponseService.toResponse(schedule);

        assertThat(response.filledSeats()).isEqualTo(1);
        assertThat(response.waitlistCount()).isEqualTo(1);
        assertThat(response.seatLimit()).isEqualTo(32);
        assertThat(response.remainingSeats()).isEqualTo(31);
    }

    @Test
    @DisplayName("should default seat counts to zero when no enrollments exist")
    void shouldDefaultSeatCountsToZeroWhenNoEnrollmentsExist() {
        when(enrollmentRepository.findByScheduleIdIn(List.of(200L))).thenReturn(List.of());

        ScheduleResponse response = scheduleResponseService.toResponse(schedule);

        assertThat(response.filledSeats()).isZero();
        assertThat(response.waitlistCount()).isZero();
        assertThat(response.seatLimit()).isEqualTo(32);
        assertThat(response.remainingSeats()).isEqualTo(32);
    }
}
