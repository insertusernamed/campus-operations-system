package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentAssignmentServiceTest {

    private static final String SPRING_2026 = "Spring 2026";

    private EnrollmentAssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new EnrollmentAssignmentService();
    }

    @Test
    @DisplayName("assigns matching schedules in ranked preference order up to target load")
    void assignsMatchingSchedulesInRankedPreferenceOrderUpToTargetLoad() {
        Course cs410 = course(10L, "CS410");
        Course math220 = course(11L, "MATH220");
        Course hist101 = course(12L, "HIST101");

        Student student = student(1L, "S00000002", 2, List.of(math220.getId(), cs410.getId(), hist101.getId()));

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(student),
                List.of(
                        schedule(100L, cs410, SPRING_2026),
                        schedule(101L, math220, SPRING_2026),
                        schedule(102L, hist101, SPRING_2026)),
                SPRING_2026);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getCourse().getCode())
                .containsExactly("MATH220", "CS410");
        assertThat(enrollments)
                .extracting(Enrollment::getStatus)
                .containsOnly(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("ignores schedules from other semesters and unmatched preferences")
    void ignoresSchedulesFromOtherSemestersAndUnmatchedPreferences() {
        Course cs410 = course(10L, "CS410");
        Course math220 = course(11L, "MATH220");

        Student student = student(1L, "S00000001", 3, List.of(cs410.getId(), 999L, math220.getId()));

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(student),
                List.of(
                        schedule(100L, cs410, "Fall 2025"),
                        schedule(101L, math220, SPRING_2026)),
                SPRING_2026);

        assertThat(enrollments).hasSize(1);
        Enrollment enrollment = enrollments.getFirst();
        assertThat(enrollment.getCourse().getCode()).isEqualTo("MATH220");
        assertThat(enrollment.getSemester()).isEqualTo(SPRING_2026);
    }

    @Test
    @DisplayName("uses deterministic student ordering and ignores duplicate course requests")
    void usesDeterministicStudentOrderingAndIgnoresDuplicateCourseRequests() {
        Course cs410 = course(10L, "CS410");
        Course math220 = course(11L, "MATH220");

        Student laterStudent = student(2L, "S00000002", 2, List.of(cs410.getId(), cs410.getId()));
        Student earlierStudent = student(1L, "S00000001", 1, List.of(math220.getId()));

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(laterStudent, earlierStudent),
                List.of(
                        schedule(100L, cs410, SPRING_2026),
                        schedule(101L, math220, SPRING_2026)),
                SPRING_2026);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getStudent().getStudentNumber())
                .containsExactly("S00000001", "S00000002");
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getCourse().getCode())
                .containsExactly("MATH220", "CS410");
    }

    private Course course(Long id, String code) {
        return Course.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .credits(3)
                .enrollmentCapacity(40)
                .department("Computer Science")
                .build();
    }

    private Schedule schedule(Long id, Course course, String semester) {
        return Schedule.builder()
                .id(id)
                .course(course)
                .semester(semester)
                .build();
    }

    private Student student(Long id, String studentNumber, int targetCourseLoad, List<Long> preferredCourseIds) {
        return Student.builder()
                .id(id)
                .studentNumber(studentNumber)
                .firstName("Test")
                .lastName("Student")
                .email(studentNumber.toLowerCase() + "@students.campusscheduler.edu")
                .department("Computer Science")
                .yearLevel(2)
                .targetCourseLoad(targetCourseLoad)
                .preferredCourseIds(preferredCourseIds)
                .build();
    }
}
