package org.campusscheduler.domain.building;

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
import org.campusscheduler.config.SecurityConfig;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
 * Controller tests for BuildingController using MockMvc.
 */
@WebMvcTest(BuildingController.class)
@Import(SecurityConfig.class)
class BuildingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuildingService buildingService;

    private Building testBuilding;

    @BeforeEach
    void setUp() {
        testBuilding = Building.builder()
                .id(1L)
                .name("Science Building")
                .code("SCI")
                .address("123 Campus Drive")
                .build();
    }

    @Nested
    @DisplayName("GET /api/buildings")
    class GetAllBuildings {

        @Test
        @DisplayName("should return all buildings")
        void shouldReturnAllBuildings() throws Exception {
            Building building2 = Building.builder()
                    .id(2L)
                    .name("Arts Building")
                    .code("ART")
                    .build();

            when(buildingService.findAll()).thenReturn(List.of(testBuilding, building2));

            mockMvc.perform(get("/api/buildings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].code", is("SCI")))
                    .andExpect(jsonPath("$[1].code", is("ART")));
        }

        @Test
        @DisplayName("should return empty list when no buildings")
        void shouldReturnEmptyListWhenNoBuildings() throws Exception {
            when(buildingService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/buildings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/buildings/{id}")
    class GetBuildingById {

        @Test
        @DisplayName("should return building when found")
        void shouldReturnBuildingWhenFound() throws Exception {
            when(buildingService.findById(1L)).thenReturn(Optional.of(testBuilding));

            mockMvc.perform(get("/api/buildings/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Science Building")))
                    .andExpect(jsonPath("$.code", is("SCI")));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(buildingService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/buildings/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/buildings")
    class CreateBuilding {

        @Test
        @DisplayName("should create building and return 201")
        void shouldCreateBuildingAndReturn201() throws Exception {
            Building savedBuilding = Building.builder()
                    .id(2L)
                    .name("New Building")
                    .code("NEW")
                    .build();

            when(buildingService.create(any(Building.class))).thenReturn(savedBuilding);

            mockMvc.perform(post("/api/buildings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"New Building\",\"code\":\"NEW\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.code", is("NEW")));
        }

        @Test
        @DisplayName("should return structured validation errors")
        void shouldReturnStructuredValidationErrors() throws Exception {
            mockMvc.perform(post("/api/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"code\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.message", not("")))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                    .andExpect(jsonPath("$.fieldErrors[0].field", not("")))
                    .andExpect(jsonPath("$.fieldErrors[0].message", not("")));
        }

        @Test
        @DisplayName("should return 400 with error message when duplicate code")
        void shouldReturn400WithErrorMessageWhenDuplicateCode() throws Exception {
            when(buildingService.create(any(Building.class)))
                    .thenThrow(new IllegalArgumentException("Building code already exists: SCI"));

            mockMvc.perform(post("/api/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Science Building\",\"code\":\"SCI\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.message", is("Building code already exists: SCI")))
                    .andExpect(jsonPath("$.error", is("Building code already exists: SCI")));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/buildings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\",\"code\":\"INV\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when code is blank")
        void shouldReturn400WhenCodeIsBlank() throws Exception {
            mockMvc.perform(post("/api/buildings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Valid Name\",\"code\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/buildings/{id}")
    class UpdateBuilding {

        @Test
        @DisplayName("should update building when found")
        void shouldUpdateBuildingWhenFound() throws Exception {
            Building updated = Building.builder()
                    .id(1L)
                    .name("Updated Name")
                    .code("SCI")
                    .address("New Address")
                    .build();

            when(buildingService.update(eq(1L), any(Building.class))).thenReturn(Optional.of(updated));

            mockMvc.perform(put("/api/buildings/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Updated Name\",\"code\":\"SCI\",\"address\":\"New Address\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Name")));
        }

        @Test
        @DisplayName("should return 404 when building not found")
        void shouldReturn404WhenBuildingNotFound() throws Exception {
            when(buildingService.update(eq(999L), any(Building.class))).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/buildings/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test\",\"code\":\"TST\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/buildings/{id}")
    class DeleteBuilding {

        @Test
        @DisplayName("should return 204 when deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            when(buildingService.delete(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/buildings/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(buildingService.delete(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/buildings/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
