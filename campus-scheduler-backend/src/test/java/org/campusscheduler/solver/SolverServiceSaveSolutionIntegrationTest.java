package org.campusscheduler.solver;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentAssignmentService;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class SolverServiceSaveSolutionIntegrationTest {

    private static final String SPRING_2026 = "Spring 2026";
    private static final String FALL_2026 = "Fall 2026";

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    private SolverService solverService;
    private Student firstStudent;
    private Student secondStudent;
    private Course algorithms;
    private Course databases;
    private Course archivedCourse;
    private Course fallCourse;
    private Room smallRoom;
    private Room largeRoom;
    private TimeSlot mondayMorning;
    private TimeSlot tuesdayMorning;

    @BeforeEach
    void setUp() {
        solverService = new SolverService(
                mock(SolverManagerConfiguration.class),
                courseRepository,
                roomRepository,
                timeSlotRepository,
                scheduleRepository,
                enrollmentRepository,
                studentRepository,
                new EnrollmentAssignmentService(),
                mock(SimpMessagingTemplate.class));

        Building building = buildingRepository.save(Building.builder()
                .code("SCI")
                .name("Science Building")
                .build());

        smallRoom = roomRepository.save(Room.builder()
                .roomNumber("101")
                .capacity(1)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build());

        largeRoom = roomRepository.save(Room.builder()
                .roomNumber("201")
                .capacity(30)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build());

        mondayMorning = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .label("Monday 9 AM")
                .build());

        tuesdayMorning = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .label("Tuesday 11 AM")
                .build());

        TimeSlot wednesdayMorning = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .label("Wednesday 8 AM")
                .build());

        algorithms = courseRepository.save(Course.builder()
                .code("CS301")
                .name("Algorithms")
                .credits(3)
                .enrollmentCapacity(3)
                .department("Computer Science")
                .build());

        databases = courseRepository.save(Course.builder()
                .code("CS330")
                .name("Databases")
                .credits(3)
                .enrollmentCapacity(3)
                .department("Computer Science")
                .build());

        archivedCourse = courseRepository.save(Course.builder()
                .code("CS340")
                .name("Legacy Spring Course")
                .credits(3)
                .enrollmentCapacity(2)
                .department("Computer Science")
                .build());

        fallCourse = courseRepository.save(Course.builder()
                .code("CS350")
                .name("Fall Course")
                .credits(3)
                .enrollmentCapacity(2)
                .department("Computer Science")
                .build());

        firstStudent = studentRepository.save(Student.builder()
                .studentNumber("S00000001")
                .firstName("Mia")
                .lastName("Lopez")
                .email("mia.lopez@students.campusscheduler.edu")
                .department("Computer Science")
                .yearLevel(2)
                .targetCourseLoad(2)
                .preferredCourseIds(List.of(algorithms.getId(), databases.getId()))
                .build());

        secondStudent = studentRepository.save(Student.builder()
                .studentNumber("S00000002")
                .firstName("Noah")
                .lastName("Patel")
                .email("noah.patel@students.campusscheduler.edu")
                .department("Computer Science")
                .yearLevel(2)
                .targetCourseLoad(1)
                .preferredCourseIds(List.of(algorithms.getId()))
                .build());

        Schedule archivedSchedule = scheduleRepository.save(Schedule.builder()
                .course(archivedCourse)
                .room(largeRoom)
                .timeSlot(wednesdayMorning)
                .semester(SPRING_2026)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(firstStudent)
                .schedule(archivedSchedule)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        Schedule fallSchedule = scheduleRepository.save(Schedule.builder()
                .course(fallCourse)
                .room(largeRoom)
                .timeSlot(wednesdayMorning)
                .semester(FALL_2026)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(secondStudent)
                .schedule(fallSchedule)
                .status(EnrollmentStatus.ENROLLED)
                .build());
    }

    @Test
    @DisplayName("saveSolution replaces semester schedules and persists stable rosters")
    void saveSolutionReplacesSemesterSchedulesAndPersistsStableRosters() {
        ScheduleSolution solution = ScheduleSolution.builder()
                .semester(SPRING_2026)
                .assignments(List.of(
                        assignment(algorithms, smallRoom, mondayMorning, SPRING_2026),
                        assignment(databases, largeRoom, tuesdayMorning, SPRING_2026)))
                .build();

        bestSolutionRef().set(solution);

        int savedCount = solverService.saveSolution();

        assertThat(savedCount).isEqualTo(2);
        assertThat(scheduleRepository.findBySemester(SPRING_2026))
                .extracting(schedule -> schedule.getCourse().getCode())
                .containsExactlyInAnyOrder("CS301", "CS330");
        assertThat(enrollmentRepository.findByCourseIdAndSemester(archivedCourse.getId(), SPRING_2026)).isEmpty();
        assertThat(enrollmentRepository.findByStudentIdAndSemester(firstStudent.getId(), SPRING_2026))
                .extracting(enrollment -> enrollment.getCourse().getCode(), Enrollment::getStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("CS301", EnrollmentStatus.ENROLLED),
                        org.assertj.core.api.Assertions.tuple("CS330", EnrollmentStatus.ENROLLED));
        assertThat(enrollmentRepository.findByStudentIdAndSemester(secondStudent.getId(), SPRING_2026))
                .extracting(enrollment -> enrollment.getCourse().getCode(), Enrollment::getStatus)
                .containsExactly(org.assertj.core.api.Assertions.tuple("CS301", EnrollmentStatus.WAITLISTED));
        assertThat(scheduleRepository.findBySemester(FALL_2026)).hasSize(1);
        assertThat(enrollmentRepository.findByStudentIdAndSemester(secondStudent.getId(), FALL_2026)).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    private AtomicReference<ScheduleSolution> bestSolutionRef() {
        return (AtomicReference<ScheduleSolution>) ReflectionTestUtils.getField(solverService, "bestSolution");
    }

    private ScheduleAssignment assignment(Course course, Room room, TimeSlot timeSlot, String semester) {
        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setId(course.getId());
        assignment.setCourse(course);
        assignment.setRoom(room);
        assignment.setTimeSlot(timeSlot);
        assignment.setSemester(semester);
        return assignment;
    }
}
