package org.campusscheduler.domain.course;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.instructor.Instructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for CourseController using MockMvc.
 */
@WebMvcTest(CourseController.class)
@Import(SecurityConfig.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    private Instructor testInstructor;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testInstructor = Instructor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .email("jsmith@university.edu")
                .department("Computer Science")
                .build();

        testCourse = Course.builder()
                .id(1L)
                .code("CS101")
                .name("Introduction to Programming")
                .description("Learn programming basics")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .instructor(testInstructor)
                .build();
    }

    @Nested
    @DisplayName("GET /api/courses")
    class GetAllCourses {

        @Test
        @DisplayName("should return all courses")
        void shouldReturnAllCourses() throws Exception {
            when(courseService.findAll()).thenReturn(List.of(testCourse));

            mockMvc.perform(get("/api/courses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].code", is("CS101")))
                    .andExpect(jsonPath("$[0].name", is("Introduction to Programming")));
        }

        @Test
        @DisplayName("should filter courses by department")
        void shouldFilterCoursesByDepartment() throws Exception {
            when(courseService.findByDepartment("Computer Science"))
                    .thenReturn(List.of(testCourse));

            mockMvc.perform(get("/api/courses").param("department", "Computer Science"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].department", is("Computer Science")));
        }

        @Test
        @DisplayName("should filter courses by instructor")
        void shouldFilterCoursesByInstructor() throws Exception {
            when(courseService.findByInstructorId(1L)).thenReturn(List.of(testCourse));

            mockMvc.perform(get("/api/courses").param("instructorId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/courses/{id}")
    class GetCourseById {

        @Test
        @DisplayName("should return course when found")
        void shouldReturnCourseWhenFound() throws Exception {
            when(courseService.findById(1L)).thenReturn(Optional.of(testCourse));

            mockMvc.perform(get("/api/courses/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.code", is("CS101")))
                    .andExpect(jsonPath("$.credits", is(3)));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(courseService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/courses/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/courses/code/{code}")
    class GetCourseByCode {

        @Test
        @DisplayName("should return course when found by code")
        void shouldReturnCourseWhenFoundByCode() throws Exception {
            when(courseService.findByCode("CS101")).thenReturn(Optional.of(testCourse));

            mockMvc.perform(get("/api/courses/code/CS101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("CS101")));
        }

        @Test
        @DisplayName("should return 404 when code not found")
        void shouldReturn404WhenCodeNotFound() throws Exception {
            when(courseService.findByCode("INVALID")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/courses/code/INVALID"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/courses")
    class CreateCourse {

        @Test
        @DisplayName("should create course and return 201")
        void shouldCreateCourseAndReturn201() throws Exception {
            Course savedCourse = Course.builder()
                    .id(2L)
                    .code("CS201")
                    .name("Data Structures")
                    .credits(3)
                    .enrollmentCapacity(25)
                    .build();

            when(courseService.create(any(Course.class))).thenReturn(savedCourse);

            mockMvc.perform(post("/api/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"code\":\"CS201\",\"name\":\"Data Structures\",\"credits\":3,\"enrollmentCapacity\":25}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.code", is("CS201")));
        }

        @Test
        @DisplayName("should return 400 when code is blank")
        void shouldReturn400WhenCodeIsBlank() throws Exception {
            mockMvc.perform(post("/api/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"\",\"name\":\"Data Structures\",\"credits\":3,\"enrollmentCapacity\":25}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when credits are invalid")
        void shouldReturn400WhenCreditsAreInvalid() throws Exception {
            mockMvc.perform(post("/api/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"code\":\"CS201\",\"name\":\"Data Structures\",\"credits\":0,\"enrollmentCapacity\":25}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/courses/instructor/{instructorId}")
    class CreateCourseWithInstructor {

        @Test
        @DisplayName("should create course with instructor and return 201")
        void shouldCreateCourseWithInstructorAndReturn201() throws Exception {
            Course savedCourse = Course.builder()
                    .id(2L)
                    .code("CS201")
                    .name("Data Structures")
                    .credits(3)
                    .enrollmentCapacity(25)
                    .instructor(testInstructor)
                    .build();

            when(courseService.createWithInstructor(any(Course.class), eq(1L)))
                    .thenReturn(Optional.of(savedCourse));

            mockMvc.perform(post("/api/courses/instructor/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"code\":\"CS201\",\"name\":\"Data Structures\",\"credits\":3,\"enrollmentCapacity\":25}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.instructor.id", is(1)));
        }

        @Test
        @DisplayName("should return 404 when instructor not found")
        void shouldReturn404WhenInstructorNotFound() throws Exception {
            when(courseService.createWithInstructor(any(Course.class), eq(999L)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/courses/instructor/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"code\":\"CS201\",\"name\":\"Data Structures\",\"credits\":3,\"enrollmentCapacity\":25}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/courses/{id}")
    class UpdateCourse {

        @Test
        @DisplayName("should update course when found")
        void shouldUpdateCourseWhenFound() throws Exception {
            Course updated = Course.builder()
                    .id(1L)
                    .code("CS101-A")
                    .name("Intro to Programming - Advanced")
                    .credits(4)
                    .enrollmentCapacity(35)
                    .department("Computer Science")
                    .build();

            when(courseService.update(eq(1L), any(Course.class))).thenReturn(Optional.of(updated));

            mockMvc.perform(put("/api/courses/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"code\":\"CS101-A\",\"name\":\"Intro to Programming - Advanced\",\"credits\":4,\"enrollmentCapacity\":35}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("CS101-A")))
                    .andExpect(jsonPath("$.credits", is(4)));
        }

        @Test
        @DisplayName("should return 404 when course not found")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            when(courseService.update(eq(999L), any(Course.class))).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/courses/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"CS101\",\"name\":\"Test\",\"credits\":3,\"enrollmentCapacity\":25}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/courses/{id}/instructor/{instructorId}")
    class AssignInstructor {

        @Test
        @DisplayName("should assign instructor and return 200")
        void shouldAssignInstructorAndReturn200() throws Exception {
            Course updatedCourse = Course.builder()
                    .id(1L)
                    .code("CS101")
                    .name("Introduction to Programming")
                    .credits(3)
                    .enrollmentCapacity(30)
                    .instructor(testInstructor)
                    .build();

            when(courseService.assignInstructor(1L, 1L)).thenReturn(Optional.of(updatedCourse));

            mockMvc.perform(put("/api/courses/1/instructor/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instructor.id", is(1)));
        }

        @Test
        @DisplayName("should return 404 when course or instructor not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(courseService.assignInstructor(999L, 1L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/courses/999/instructor/1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/courses/{id}")
    class DeleteCourse {

        @Test
        @DisplayName("should return 204 when deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            when(courseService.delete(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/courses/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(courseService.delete(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/courses/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
