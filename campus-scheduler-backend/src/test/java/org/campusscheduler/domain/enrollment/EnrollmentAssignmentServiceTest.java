package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
                        schedule(100L, cs410, 30, SPRING_2026),
                        schedule(101L, math220, 30, SPRING_2026),
                        schedule(102L, hist101, 30, SPRING_2026)),
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
                        schedule(100L, cs410, 30, "Fall 2025"),
                        schedule(101L, math220, 30, SPRING_2026)),
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
                        schedule(100L, cs410, 30, SPRING_2026),
                        schedule(101L, math220, 30, SPRING_2026)),
                SPRING_2026);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getStudent().getStudentNumber())
                .containsExactly("S00000001", "S00000002");
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getCourse().getCode())
                .containsExactly("MATH220", "CS410");
    }

    @Test
    @DisplayName("fills all seats exactly when demand matches computed seat limit")
    void fillsAllSeatsExactlyWhenDemandMatchesComputedSeatLimit() {
        Course cs410 = course(10L, "CS410", 3);
        Schedule cs410Schedule = schedule(100L, cs410, 2, SPRING_2026);

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(
                        student(1L, "S00000001", 1, List.of(cs410.getId())),
                        student(2L, "S00000002", 1, List.of(cs410.getId()))),
                List.of(cs410Schedule),
                SPRING_2026);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments)
                .extracting(Enrollment::getStatus)
                .containsExactly(EnrollmentStatus.ENROLLED, EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("places overflow onto waitlist using the lower of room and course capacity")
    void placesOverflowOntoWaitlistUsingTheLowerOfRoomAndCourseCapacity() {
        Course cs410 = course(10L, "CS410", 4);
        Course math220 = course(11L, "MATH220", 4);
        Schedule cs410Schedule = schedule(100L, cs410, 2, SPRING_2026);
        Schedule math220Schedule = schedule(101L, math220, 5, SPRING_2026);

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(
                        student(2L, "S00000002", 1, List.of(cs410.getId())),
                        student(1L, "S00000001", 1, List.of(cs410.getId())),
                        student(3L, "S00000003", 2, List.of(cs410.getId(), math220.getId()))),
                List.of(cs410Schedule, math220Schedule),
                SPRING_2026);

        assertThat(enrollments).hasSize(4);
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getStudent().getStudentNumber(), Enrollment::getStatus)
                .containsExactly(
                        tuple("S00000001", EnrollmentStatus.ENROLLED),
                        tuple("S00000002", EnrollmentStatus.ENROLLED),
                        tuple("S00000003", EnrollmentStatus.WAITLISTED),
                        tuple("S00000003", EnrollmentStatus.ENROLLED));
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getCourse().getCode())
                .containsExactly("CS410", "CS410", "CS410", "MATH220");
    }

    @Test
    @DisplayName("skips overlapping schedules and keeps later non-conflicting preferences")
    void skipsOverlappingSchedulesAndKeepsLaterNonConflictingPreferences() {
        Course cs410 = course(10L, "CS410");
        Course math220 = course(11L, "MATH220");
        Course hist101 = course(12L, "HIST101");

        Student student = student(1L, "S00000001", 2, List.of(cs410.getId(), math220.getId(), hist101.getId()));

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(student),
                List.of(
                        schedule(100L, cs410, 30, SPRING_2026, DayOfWeek.MONDAY, 9, 0, 10, 0),
                        schedule(101L, math220, 30, SPRING_2026, DayOfWeek.MONDAY, 9, 30, 10, 30),
                        schedule(102L, hist101, 30, SPRING_2026, DayOfWeek.TUESDAY, 11, 0, 12, 0)),
                SPRING_2026);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getCourse().getCode())
                .containsExactly("CS410", "HIST101");
    }

    @Test
    @DisplayName("enforces the three classes per day cap and continues to other days")
    void enforcesTheThreeClassesPerDayCapAndContinuesToOtherDays() {
        Course cs101 = course(10L, "CS101");
        Course cs102 = course(11L, "CS102");
        Course cs103 = course(12L, "CS103");
        Course cs104 = course(13L, "CS104");
        Course cs105 = course(14L, "CS105");

        Student student = student(
                1L,
                "S00000001",
                4,
                List.of(cs101.getId(), cs102.getId(), cs103.getId(), cs104.getId(), cs105.getId()));

        List<Enrollment> enrollments = assignmentService.assignEnrollments(
                List.of(student),
                List.of(
                        schedule(100L, cs101, 30, SPRING_2026, DayOfWeek.MONDAY, 8, 0, 9, 0),
                        schedule(101L, cs102, 30, SPRING_2026, DayOfWeek.MONDAY, 9, 15, 10, 15),
                        schedule(102L, cs103, 30, SPRING_2026, DayOfWeek.MONDAY, 10, 30, 11, 30),
                        schedule(103L, cs104, 30, SPRING_2026, DayOfWeek.MONDAY, 12, 0, 13, 0),
                        schedule(104L, cs105, 30, SPRING_2026, DayOfWeek.TUESDAY, 9, 0, 10, 0)),
                SPRING_2026);

        assertThat(enrollments).hasSize(4);
        assertThat(enrollments)
                .extracting(enrollment -> enrollment.getCourse().getCode())
                .containsExactly("CS101", "CS102", "CS103", "CS105");
    }

    private Course course(Long id, String code) {
        return course(id, code, 40);
    }

    private Course course(Long id, String code, int enrollmentCapacity) {
        return Course.builder()
                .id(id)
                .code(code)
                .name(code + " Name")
                .credits(3)
                .enrollmentCapacity(enrollmentCapacity)
                .department("Computer Science")
                .build();
    }

    private Schedule schedule(Long id, Course course, int roomCapacity, String semester) {
        return schedule(id, course, roomCapacity, semester, null, 0, 0, 0, 0);
    }

    private Schedule schedule(
            Long id,
            Course course,
            int roomCapacity,
            String semester,
            DayOfWeek dayOfWeek,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {
        return Schedule.builder()
                .id(id)
                .course(course)
                .room(Room.builder()
                        .id(id + 1000)
                        .roomNumber("R" + id)
                        .capacity(roomCapacity)
                        .type(Room.RoomType.CLASSROOM)
                        .build())
                .timeSlot(dayOfWeek == null
                        ? null
                        : TimeSlot.builder()
                                .id(id + 2000)
                                .dayOfWeek(dayOfWeek)
                                .startTime(LocalTime.of(startHour, startMinute))
                                .endTime(LocalTime.of(endHour, endMinute))
                                .label(dayOfWeek + " " + startHour + ":" + String.format("%02d", startMinute))
                                .build())
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
