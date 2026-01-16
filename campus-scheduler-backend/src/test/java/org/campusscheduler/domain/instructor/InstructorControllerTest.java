package org.campusscheduler.domain.instructor;

import org.campusscheduler.config.SecurityConfig;
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
 * Controller tests for InstructorController using MockMvc.
 */
@WebMvcTest(InstructorController.class)
@Import(SecurityConfig.class)
class InstructorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

    private Instructor testInstructor;

    @BeforeEach
    void setUp() {
        testInstructor = Instructor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .email("jsmith@university.edu")
                .department("Computer Science")
                .officeNumber("CS-201")
                .build();
    }

    @Nested
    @DisplayName("GET /api/instructors")
    class GetAllInstructors {

        @Test
        @DisplayName("should return all instructors")
        void shouldReturnAllInstructors() throws Exception {
            when(instructorService.findAll()).thenReturn(List.of(testInstructor));

            mockMvc.perform(get("/api/instructors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].firstName", is("John")))
                    .andExpect(jsonPath("$[0].lastName", is("Smith")));
        }

        @Test
        @DisplayName("should filter instructors by department")
        void shouldFilterInstructorsByDepartment() throws Exception {
            when(instructorService.findByDepartment("Computer Science"))
                    .thenReturn(List.of(testInstructor));

            mockMvc.perform(get("/api/instructors").param("department", "Computer Science"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].department", is("Computer Science")));
        }
    }

    @Nested
    @DisplayName("GET /api/instructors/{id}")
    class GetInstructorById {

        @Test
        @DisplayName("should return instructor when found")
        void shouldReturnInstructorWhenFound() throws Exception {
            when(instructorService.findById(1L)).thenReturn(Optional.of(testInstructor));

            mockMvc.perform(get("/api/instructors/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.email", is("jsmith@university.edu")));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(instructorService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/instructors/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/instructors")
    class CreateInstructor {

        @Test
        @DisplayName("should create instructor and return 201")
        void shouldCreateInstructorAndReturn201() throws Exception {
            Instructor savedInstructor = Instructor.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Doe")
                    .email("jdoe@university.edu")
                    .department("Mathematics")
                    .build();

            when(instructorService.create(any(Instructor.class))).thenReturn(savedInstructor);

            mockMvc.perform(post("/api/instructors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jdoe@university.edu\",\"department\":\"Mathematics\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.firstName", is("Jane")));
        }

        @Test
        @DisplayName("should return 400 when firstName is blank")
        void shouldReturn400WhenFirstNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/instructors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"firstName\":\"\",\"lastName\":\"Doe\",\"email\":\"jdoe@university.edu\",\"department\":\"Mathematics\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when lastName is blank")
        void shouldReturn400WhenLastNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/instructors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"firstName\":\"Jane\",\"lastName\":\"\",\"email\":\"jdoe@university.edu\",\"department\":\"Mathematics\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            mockMvc.perform(post("/api/instructors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"invalid-email\",\"department\":\"Mathematics\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/instructors/{id}")
    class UpdateInstructor {

        @Test
        @DisplayName("should update instructor when found")
        void shouldUpdateInstructorWhenFound() throws Exception {
            Instructor updated = Instructor.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Smith Jr.")
                    .email("jsmith.jr@university.edu")
                    .department("Computer Science")
                    .officeNumber("CS-301")
                    .build();

            when(instructorService.update(eq(1L), any(Instructor.class))).thenReturn(Optional.of(updated));

            mockMvc.perform(put("/api/instructors/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"firstName\":\"John\",\"lastName\":\"Smith Jr.\",\"email\":\"jsmith.jr@university.edu\",\"department\":\"Computer Science\",\"officeNumber\":\"CS-301\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastName", is("Smith Jr.")))
                    .andExpect(jsonPath("$.officeNumber", is("CS-301")));
        }

        @Test
        @DisplayName("should return 404 when instructor not found")
        void shouldReturn404WhenInstructorNotFound() throws Exception {
            when(instructorService.update(eq(999L), any(Instructor.class))).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/instructors/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"firstName\":\"John\",\"lastName\":\"Smith\",\"email\":\"jsmith@university.edu\",\"department\":\"Computer Science\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/instructors/{id}")
    class DeleteInstructor {

        @Test
        @DisplayName("should return 204 when deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            when(instructorService.delete(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/instructors/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(instructorService.delete(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/instructors/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
