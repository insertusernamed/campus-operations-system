package org.campusscheduler.domain.student;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentService;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@Import(SecurityConfig.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    private Student student;
    private Enrollment enrolledEnrollment;
    private Enrollment waitlistedEnrollment;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .studentNumber("S300001")
                .firstName("Harper")
                .lastName("Chen")
                .email("harper.chen@student.university.edu")
                .department("Computer Science")
                .yearLevel(4)
                .targetCourseLoad(4)
                .preferredCourseIds(List.of(10L, 20L))
                .build();

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

        TimeSlot mondaySlot = TimeSlot.builder()
                .id(7L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Mon 9:00")
                .build();

        TimeSlot wednesdaySlot = TimeSlot.builder()
                .id(8L)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 30))
                .label("Wed 1:00")
                .build();

        enrolledEnrollment = Enrollment.builder()
                .id(100L)
                .student(student)
                .course(course)
                .schedule(Schedule.builder()
                        .id(200L)
                        .course(course)
                        .room(room)
                        .timeSlot(mondaySlot)
                        .semester("Fall 2026")
                        .build())
                .semester("Fall 2026")
                .status(EnrollmentStatus.ENROLLED)
                .build();

        waitlistedEnrollment = Enrollment.builder()
                .id(101L)
                .student(student)
                .course(course)
                .schedule(Schedule.builder()
                        .id(201L)
                        .course(course)
                        .room(room)
                        .timeSlot(wednesdaySlot)
                        .semester("Fall 2026")
                        .build())
                .semester("Fall 2026")
                .status(EnrollmentStatus.WAITLISTED)
                .build();
    }

    @Nested
    @DisplayName("GET /api/students")
    class GetAllStudents {

        @Test
        @DisplayName("should return all students")
        void shouldReturnAllStudents() throws Exception {
            when(studentService.findAll()).thenReturn(List.of(student));

            mockMvc.perform(get("/api/students"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].studentNumber", is("S300001")))
                    .andExpect(jsonPath("$[0].preferredCourseIds", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/students/{id}")
    class GetStudentById {

        @Test
        @DisplayName("should return student when found")
        void shouldReturnStudentWhenFound() throws Exception {
            when(studentService.findById(1L)).thenReturn(Optional.of(student));

            mockMvc.perform(get("/api/students/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.email", is("harper.chen@student.university.edu")));
        }

        @Test
        @DisplayName("should return 404 when student not found")
        void shouldReturn404WhenStudentNotFound() throws Exception {
            when(studentService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/students/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/students/{id}/schedule")
    class GetStudentSchedule {

        @Test
        @DisplayName("should return enrolled and waitlisted classes for semester")
        void shouldReturnEnrolledAndWaitlistedClassesForSemester() throws Exception {
            when(studentService.findById(1L)).thenReturn(Optional.of(student));
            when(enrollmentService.findByStudentAndSemester(1L, "Fall 2026"))
                    .thenReturn(List.of(waitlistedEnrollment, enrolledEnrollment));

            mockMvc.perform(get("/api/students/1/schedule")
                    .param("semester", "Fall 2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentId", is(1)))
                    .andExpect(jsonPath("$.semester", is("Fall 2026")))
                    .andExpect(jsonPath("$.enrolled", hasSize(1)))
                    .andExpect(jsonPath("$.waitlisted", hasSize(1)))
                    .andExpect(jsonPath("$.enrolled[0].status", is("ENROLLED")))
                    .andExpect(jsonPath("$.enrolled[0].schedule.course.code", is("CS410")))
                    .andExpect(jsonPath("$.waitlisted[0].status", is("WAITLISTED")));
        }

        @Test
        @DisplayName("should return 404 when student is missing")
        void shouldReturn404WhenStudentIsMissing() throws Exception {
            when(studentService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/students/999/schedule")
                    .param("semester", "Fall 2026"))
                    .andExpect(status().isNotFound());
        }
    }
}
