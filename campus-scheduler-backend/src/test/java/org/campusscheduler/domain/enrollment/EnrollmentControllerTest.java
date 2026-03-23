package org.campusscheduler.domain.enrollment;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.student.Student;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@Import(SecurityConfig.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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

        TimeSlot timeSlot = TimeSlot.builder()
                .id(7L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Mon 9:00")
                .build();

        Schedule schedule = Schedule.builder()
                .id(200L)
                .course(course)
                .room(room)
                .timeSlot(timeSlot)
                .semester("Fall 2026")
                .build();

        enrollment = Enrollment.builder()
                .id(100L)
                .student(student)
                .course(course)
                .schedule(schedule)
                .semester("Fall 2026")
                .status(EnrollmentStatus.ENROLLED)
                .build();
    }

    @Nested
    @DisplayName("GET /api/enrollments")
    class GetEnrollments {

        @Test
        @DisplayName("should return all enrollments when no filters are provided")
        void shouldReturnAllEnrollmentsWhenNoFiltersAreProvided() throws Exception {
            when(enrollmentService.findByFilters(null, null, null, null)).thenReturn(List.of(enrollment));

            mockMvc.perform(get("/api/enrollments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].student.studentNumber", is("S300001")))
                    .andExpect(jsonPath("$[0].schedule.course.code", is("CS410")));
        }

        @Test
        @DisplayName("should pass through all supported filters")
        void shouldPassThroughAllSupportedFilters() throws Exception {
            when(enrollmentService.findByFilters(1L, 10L, 200L, "Fall 2026")).thenReturn(List.of(enrollment));

            mockMvc.perform(get("/api/enrollments")
                    .param("studentId", "1")
                    .param("courseId", "10")
                    .param("scheduleId", "200")
                    .param("semester", "Fall 2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("ENROLLED")))
                    .andExpect(jsonPath("$[0].semester", is("Fall 2026")))
                    .andExpect(jsonPath("$[0].schedule.id", is(200)));
        }
    }
}
