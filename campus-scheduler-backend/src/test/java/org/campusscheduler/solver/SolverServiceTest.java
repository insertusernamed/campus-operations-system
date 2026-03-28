package org.campusscheduler.solver;

import ai.timefold.solver.core.api.solver.SolverStatus;
import org.campusscheduler.domain.building.Building;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolverServiceTest {

    @Mock
    private SolverManagerConfiguration solverConfig;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentAssignmentService enrollmentAssignmentService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SolverService solverService;

    @Test
    void getAnalyticsHandlesVolatileAssignmentVariables() {
        Building building = Building.builder()
                .id(1L)
                .name("Science Building")
                .code("SCI")
                .build();

        Room room = Room.builder()
                .id(11L)
                .roomNumber("101")
                .capacity(30)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(21L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 15))
                .label("Period 1")
                .build();

        ScheduleAssignment assignment = new FlakyTimeSlotAssignment(room, timeSlot, "Winter 2026");
        ScheduleSolution solution = ScheduleSolution.builder()
                .semester("Winter 2026")
                .rooms(List.of(room))
                .timeSlots(List.of(timeSlot))
                .assignments(List.of(assignment))
                .build();

        atomicRef("bestSolution").set(solution);
        atomicRef("solverStatus").set(SolverStatus.SOLVING_ACTIVE);

        SolverService.SolverAnalyticsResponse response = solverService.getAnalytics("Winter 2026");

        assertThat(response.totalScheduledSlots()).isEqualTo(1L);
        assertThat(response.peakHours()).hasSize(1);
        assertThat(response.peakHours().getFirst().bookingCount()).isEqualTo(1L);
    }

    @Test
    void buildProblemIncludesDerivedStudentDemandFacts() {
        Course course = Course.builder()
                .id(1L)
                .code("CS101")
                .name("Intro CS")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .build();
        Student student = Student.builder()
                .id(10L)
                .studentNumber("S00000001")
                .firstName("Mia")
                .lastName("Lopez")
                .email("mia.lopez@student.test.edu")
                .department("Computer Science")
                .yearLevel(2)
                .targetCourseLoad(1)
                .preferredCourseIds(List.of(course.getId(), 999L, course.getId()))
                .build();

        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(roomRepository.findAll()).thenReturn(List.of());
        when(timeSlotRepository.findAll()).thenReturn(List.of());
        when(studentRepository.findAll()).thenReturn(List.of(student));

        ScheduleSolution solution = ReflectionTestUtils.invokeMethod(solverService, "buildProblem", "Fall 2026");

        assertThat(solution).isNotNull();
        assertThat(solution.getStudentCourseDemands())
                .singleElement()
                .satisfies(demand -> {
                    assertThat(demand.studentId()).isEqualTo(10L);
                    assertThat(demand.courseId()).isEqualTo(1L);
                    assertThat(demand.primaryRequest()).isTrue();
                    assertThat(demand.highPriorityRequest()).isTrue();
                });
        assertThat(solution.getCourseDemandSummaries())
                .containsExactly(new CourseDemandSummary(1L, 1, 1, 1));
    }

    @Test
    void getAnalyticsIncludesStudentMetricsFromSavedEnrollments() {
        Building building = Building.builder()
                .id(1L)
                .name("Science Building")
                .code("SCI")
                .build();

        Room room = Room.builder()
                .id(11L)
                .roomNumber("101")
                .capacity(1)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build();

        TimeSlot firstSlot = TimeSlot.builder()
                .id(21L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .label("Period 1")
                .build();

        TimeSlot secondSlot = TimeSlot.builder()
                .id(22L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .label("Period 2")
                .build();

        Course firstCourse = Course.builder()
                .id(1L)
                .code("CS101")
                .name("Intro CS")
                .credits(3)
                .enrollmentCapacity(1)
                .department("Computer Science")
                .build();

        Course secondCourse = Course.builder()
                .id(2L)
                .code("CS102")
                .name("Data Structures")
                .credits(3)
                .enrollmentCapacity(1)
                .department("Computer Science")
                .build();

        Schedule firstSchedule = Schedule.builder()
                .id(101L)
                .course(firstCourse)
                .room(room)
                .timeSlot(firstSlot)
                .semester("Fall 2026")
                .build();

        Schedule secondSchedule = Schedule.builder()
                .id(102L)
                .course(secondCourse)
                .room(room)
                .timeSlot(secondSlot)
                .semester("Fall 2026")
                .build();

        Student student = Student.builder()
                .id(10L)
                .studentNumber("S00000001")
                .firstName("Mia")
                .lastName("Lopez")
                .email("mia.lopez@student.test.edu")
                .department("Computer Science")
                .yearLevel(2)
                .preferredCourseIds(List.of(1L, 2L))
                .build();

        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(timeSlotRepository.findAll()).thenReturn(List.of(firstSlot, secondSlot));
        when(scheduleRepository.findBySemester("Fall 2026")).thenReturn(List.of(firstSchedule, secondSchedule));
        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(enrollmentRepository.findBySemester("Fall 2026")).thenReturn(List.of(
                Enrollment.builder()
                        .id(1001L)
                        .student(student)
                        .course(firstCourse)
                        .schedule(firstSchedule)
                        .semester("Fall 2026")
                        .status(EnrollmentStatus.ENROLLED)
                        .build(),
                Enrollment.builder()
                        .id(1002L)
                        .student(student)
                        .course(secondCourse)
                        .schedule(secondSchedule)
                        .semester("Fall 2026")
                        .status(EnrollmentStatus.ENROLLED)
                        .build(),
                Enrollment.builder()
                        .id(1003L)
                        .student(student)
                        .course(secondCourse)
                        .schedule(secondSchedule)
                        .semester("Fall 2026")
                        .status(EnrollmentStatus.WAITLISTED)
                        .build()));

        SolverService.SolverAnalyticsResponse response = solverService.getAnalytics("Fall 2026");

        assertThat(response.totalStudents()).isEqualTo(1);
        assertThat(response.enrolledRequests()).isEqualTo(2);
        assertThat(response.waitlistedRequests()).isEqualTo(1);
        assertThat(response.averageFillRate()).isEqualTo(100.0);
        assertThat(response.averageGapMinutes()).isEqualTo(60.0);
        assertThat(response.averageActiveDaysPerStudent()).isEqualTo(1.0);
        assertThat(response.dailyLoadDistribution())
                .containsExactly(new SolverService.SolverStudentDailyLoad(2, 1));
        assertThat(response.highDemandCourses())
                .isNotEmpty()
                .first()
                .satisfies(row -> {
                    assertThat(row.courseCode()).isEqualTo("CS102");
                    assertThat(row.waitlistCount()).isEqualTo(1);
                    assertThat(row.demandPressurePercentage()).isEqualTo(200.0);
                });
        assertThat(response.worstWaitlists())
                .containsExactlyElementsOf(response.highDemandCourses().stream()
                        .filter(row -> row.waitlistCount() > 0)
                        .limit(1)
                        .toList());
    }

    @SuppressWarnings("unchecked")
    private <T> AtomicReference<T> atomicRef(String fieldName) {
        return (AtomicReference<T>) ReflectionTestUtils.getField(solverService, fieldName);
    }

    /**
     * Simulates assignment variables changing between repeated getter calls while
     * analytics are read concurrently with solving.
     */
    private static final class FlakyTimeSlotAssignment extends ScheduleAssignment {
        private boolean firstRead = true;

        private FlakyTimeSlotAssignment(Room room, TimeSlot timeSlot, String semester) {
            setId(1L);
            setRoom(room);
            setTimeSlot(timeSlot);
            setSemester(semester);
        }

        @Override
        public TimeSlot getTimeSlot() {
            if (firstRead) {
                firstRead = false;
                return super.getTimeSlot();
            }
            return null;
        }
    }
}
