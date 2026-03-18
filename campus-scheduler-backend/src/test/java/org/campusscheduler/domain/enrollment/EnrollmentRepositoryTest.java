package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.student.StudentRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository integration tests for Enrollment entity.
 */
@DataJpaTest
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private Student studentOne;
    private Student studentTwo;
    private Course courseOne;
    private Course courseTwo;
    private Schedule springSchedule;
    private Schedule fallSchedule;

    @BeforeEach
    void setUp() {
        studentOne = studentRepository.save(Student.builder()
                .studentNumber("S200001")
                .firstName("Mia")
                .lastName("Hernandez")
                .email("mia.hernandez@student.university.edu")
                .department("Computer Science")
                .yearLevel(2)
                .build());

        studentTwo = studentRepository.save(Student.builder()
                .studentNumber("S200002")
                .firstName("Ethan")
                .lastName("Ross")
                .email("ethan.ross@student.university.edu")
                .department("Mathematics")
                .yearLevel(3)
                .build());

        courseOne = courseRepository.save(Course.builder()
                .code("CS301")
                .name("Algorithms")
                .credits(3)
                .enrollmentCapacity(35)
                .department("Computer Science")
                .build());

        courseTwo = courseRepository.save(Course.builder()
                .code("MATH220")
                .name("Discrete Math")
                .credits(3)
                .enrollmentCapacity(40)
                .department("Mathematics")
                .build());

        Building building = buildingRepository.save(Building.builder()
                .code("ENG")
                .name("Engineering Hall")
                .build());

        Room room = roomRepository.save(Room.builder()
                .roomNumber("210")
                .capacity(50)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build());

        TimeSlot morningSlot = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Morning")
                .build());

        TimeSlot afternoonSlot = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 30))
                .label("Afternoon")
                .build());

        springSchedule = scheduleRepository.save(Schedule.builder()
                .course(courseOne)
                .room(room)
                .timeSlot(morningSlot)
                .semester("Spring 2026")
                .build());

        fallSchedule = scheduleRepository.save(Schedule.builder()
                .course(courseTwo)
                .room(room)
                .timeSlot(afternoonSlot)
                .semester("Fall 2026")
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(studentOne)
                .course(courseOne)
                .schedule(springSchedule)
                .semester("Spring 2026")
                .status(EnrollmentStatus.ENROLLED)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(studentTwo)
                .course(courseTwo)
                .schedule(fallSchedule)
                .semester("Fall 2026")
                .status(EnrollmentStatus.WAITLISTED)
                .build());
    }

    @Test
    @DisplayName("should find enrollments by student and semester")
    void shouldFindEnrollmentsByStudentAndSemester() {
        List<Enrollment> result = enrollmentRepository.findByStudentIdAndSemester(
                studentOne.getId(),
                "Spring 2026"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("should find enrollments by student")
    void shouldFindEnrollmentsByStudent() {
        List<Enrollment> result = enrollmentRepository.findByStudentId(studentOne.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSchedule().getId()).isEqualTo(springSchedule.getId());
    }

    @Test
    @DisplayName("should find enrollments by course and semester")
    void shouldFindEnrollmentsByCourseAndSemester() {
        List<Enrollment> result = enrollmentRepository.findByCourseIdAndSemester(
                courseTwo.getId(),
                "Fall 2026"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudent().getStudentNumber()).isEqualTo("S200002");
    }

    @Test
    @DisplayName("should find enrollments by schedule and semester")
    void shouldFindEnrollmentsByScheduleAndSemester() {
        List<Enrollment> result = enrollmentRepository.findByScheduleIdAndSemester(
                springSchedule.getId(),
                "Spring 2026"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourse().getCode()).isEqualTo("CS301");
    }

    @Test
    @DisplayName("should find enrollments by student course and semester")
    void shouldFindEnrollmentsByStudentCourseAndSemester() {
        List<Enrollment> result = enrollmentRepository.findByStudentIdAndCourseIdAndSemester(
                studentOne.getId(),
                courseOne.getId(),
                "Spring 2026"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSchedule().getId()).isEqualTo(springSchedule.getId());
    }

    @Test
    @DisplayName("should find enrollments by student schedule and semester")
    void shouldFindEnrollmentsByStudentScheduleAndSemester() {
        List<Enrollment> result = enrollmentRepository.findByStudentIdAndScheduleIdAndSemester(
                studentOne.getId(),
                springSchedule.getId(),
                "Spring 2026"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourse().getId()).isEqualTo(courseOne.getId());
    }

    @Test
    @DisplayName("should save enrollment with waitlist status")
    void shouldSaveEnrollmentWithWaitlistStatus() {
        Student transferStudent = studentRepository.save(Student.builder()
                .studentNumber("S200003")
                .firstName("Nora")
                .lastName("Kim")
                .email("nora.kim@student.university.edu")
                .department("Computer Science")
                .yearLevel(1)
                .build());

        Enrollment saved = enrollmentRepository.saveAndFlush(Enrollment.builder()
                .student(transferStudent)
                .course(courseOne)
                .schedule(springSchedule)
                .semester("Spring 2026")
                .status(EnrollmentStatus.WAITLISTED)
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
    }

    @Test
    @DisplayName("should derive course and semester from schedule")
    void shouldDeriveCourseAndSemesterFromSchedule() {
        Student transferStudent = studentRepository.save(Student.builder()
                .studentNumber("S200004")
                .firstName("Iris")
                .lastName("Lopez")
                .email("iris.lopez@student.university.edu")
                .department("Computer Science")
                .yearLevel(2)
                .build());

        Enrollment saved = enrollmentRepository.saveAndFlush(Enrollment.builder()
                .student(transferStudent)
                .course(courseTwo)
                .schedule(springSchedule)
                .semester("Fall 2026")
                .status(EnrollmentStatus.ENROLLED)
                .build());

        assertThat(saved.getCourse().getId()).isEqualTo(courseOne.getId());
        assertThat(saved.getSemester()).isEqualTo("Spring 2026");
    }

    @Test
    @DisplayName("should enforce unique student schedule enrollment")
    void shouldEnforceUniqueStudentScheduleEnrollment() {
        Enrollment duplicate = Enrollment.builder()
                .student(studentOne)
                .course(courseOne)
                .schedule(springSchedule)
                .semester("Spring 2026")
                .status(EnrollmentStatus.WAITLISTED)
                .build();

        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
