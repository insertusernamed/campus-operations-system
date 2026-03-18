package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EnrollmentService.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        Student student = Student.builder()
                .id(1L)
                .studentNumber("S300001")
                .firstName("Harper")
                .lastName("Chen")
                .email("harper.chen@student.university.edu")
                .department("Computer Science")
                .yearLevel(4)
                .build();

        Course course = Course.builder()
                .id(10L)
                .code("CS410")
                .name("Distributed Systems")
                .credits(3)
                .enrollmentCapacity(40)
                .department("Computer Science")
                .build();

        Schedule schedule = Schedule.builder()
                .id(20L)
                .course(course)
                .semester("Spring 2026")
                .build();

        enrollment = Enrollment.builder()
                .id(100L)
                .student(student)
                .course(course)
                .schedule(schedule)
                .semester("Spring 2026")
                .status(EnrollmentStatus.ENROLLED)
                .build();
    }

    @Nested
    @DisplayName("findByStudent")
    class FindByStudent {

        @Test
        @DisplayName("should return enrollments by student")
        void shouldReturnEnrollmentsByStudent() {
            when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByStudent(1L);

            assertThat(result).containsExactly(enrollment);
            verify(enrollmentRepository).findByStudentId(1L);
        }
    }

    @Nested
    @DisplayName("semester queries")
    class SemesterQueries {

        @Test
        @DisplayName("should return enrollments by student and semester")
        void shouldReturnEnrollmentsByStudentAndSemester() {
            when(enrollmentRepository.findByStudentIdAndSemester(1L, "Spring 2026"))
                    .thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByStudentAndSemester(1L, "Spring 2026");

            assertThat(result).containsExactly(enrollment);
        }

        @Test
        @DisplayName("should return enrollments by course and semester")
        void shouldReturnEnrollmentsByCourseAndSemester() {
            when(enrollmentRepository.findByCourseIdAndSemester(10L, "Spring 2026"))
                    .thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByCourseAndSemester(10L, "Spring 2026");

            assertThat(result).containsExactly(enrollment);
        }

        @Test
        @DisplayName("should return enrollments by schedule and semester")
        void shouldReturnEnrollmentsByScheduleAndSemester() {
            when(enrollmentRepository.findByScheduleIdAndSemester(20L, "Spring 2026"))
                    .thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByScheduleAndSemester(20L, "Spring 2026");

            assertThat(result).containsExactly(enrollment);
        }

        @Test
        @DisplayName("should return enrollments by student course and semester")
        void shouldReturnEnrollmentsByStudentCourseAndSemester() {
            when(enrollmentRepository.findByStudentIdAndCourseIdAndSemester(1L, 10L, "Spring 2026"))
                    .thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByStudentCourseAndSemester(1L, 10L, "Spring 2026");

            assertThat(result).containsExactly(enrollment);
        }

        @Test
        @DisplayName("should return enrollments by student schedule and semester")
        void shouldReturnEnrollmentsByStudentScheduleAndSemester() {
            when(enrollmentRepository.findByStudentIdAndScheduleIdAndSemester(1L, 20L, "Spring 2026"))
                    .thenReturn(List.of(enrollment));

            List<Enrollment> result = enrollmentService.findByStudentScheduleAndSemester(1L, 20L, "Spring 2026");

            assertThat(result).containsExactly(enrollment);
        }
    }
}
