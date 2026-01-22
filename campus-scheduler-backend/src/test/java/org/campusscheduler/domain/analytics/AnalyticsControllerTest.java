package org.campusscheduler.domain.analytics;

import org.campusscheduler.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for AnalyticsController.
 */
@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    private static final String TEST_SEMESTER = "Spring 2026";

    @Nested
    @DisplayName("GET /api/analytics/rooms")
    class GetAllRoomsUtilization {

        @Test
        @DisplayName("should return all rooms utilization")
        void shouldReturnAllRoomsUtilization() throws Exception {
            RoomUtilizationDTO room1 = RoomUtilizationDTO.builder()
                    .roomId(1L)
                    .roomNumber("101")
                    .buildingName("Science Building")
                    .utilizationPercentage(75.0)
                    .build();

            when(analyticsService.getAllRoomsUtilization(TEST_SEMESTER))
                    .thenReturn(List.of(room1));

            mockMvc.perform(get("/api/analytics/rooms")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].roomId").value(1))
                    .andExpect(jsonPath("$[0].roomNumber").value("101"))
                    .andExpect(jsonPath("$[0].utilizationPercentage").value(75.0));
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/rooms/{id}")
    class GetRoomUtilization {

        @Test
        @DisplayName("should return room utilization when found")
        void shouldReturnRoomUtilizationWhenFound() throws Exception {
            RoomUtilizationDTO room = RoomUtilizationDTO.builder()
                    .roomId(1L)
                    .roomNumber("101")
                    .utilizationPercentage(50.0)
                    .build();

            when(analyticsService.getRoomUtilization(1L, TEST_SEMESTER))
                    .thenReturn(Optional.of(room));

            mockMvc.perform(get("/api/analytics/rooms/1")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomId").value(1))
                    .andExpect(jsonPath("$.utilizationPercentage").value(50.0));
        }

        @Test
        @DisplayName("should return 404 when room not found")
        void shouldReturn404WhenRoomNotFound() throws Exception {
            when(analyticsService.getRoomUtilization(999L, TEST_SEMESTER))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/analytics/rooms/999")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/buildings")
    class GetAllBuildingsUtilization {

        @Test
        @DisplayName("should return all buildings utilization")
        void shouldReturnAllBuildingsUtilization() throws Exception {
            BuildingUtilizationDTO building = BuildingUtilizationDTO.builder()
                    .buildingId(1L)
                    .buildingName("Science Building")
                    .utilizationPercentage(65.0)
                    .build();

            when(analyticsService.getAllBuildingsUtilization(TEST_SEMESTER))
                    .thenReturn(List.of(building));

            mockMvc.perform(get("/api/analytics/buildings")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].buildingId").value(1))
                    .andExpect(jsonPath("$[0].utilizationPercentage").value(65.0));
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/buildings/{id}")
    class GetBuildingUtilization {

        @Test
        @DisplayName("should return building utilization when found")
        void shouldReturnBuildingUtilizationWhenFound() throws Exception {
            BuildingUtilizationDTO building = BuildingUtilizationDTO.builder()
                    .buildingId(1L)
                    .buildingName("Science Building")
                    .utilizationPercentage(65.0)
                    .build();

            when(analyticsService.getBuildingUtilization(1L, TEST_SEMESTER))
                    .thenReturn(Optional.of(building));

            mockMvc.perform(get("/api/analytics/buildings/1")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buildingId").value(1));
        }

        @Test
        @DisplayName("should return 404 when building not found")
        void shouldReturn404WhenBuildingNotFound() throws Exception {
            when(analyticsService.getBuildingUtilization(999L, TEST_SEMESTER))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/analytics/buildings/999")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/peak-hours")
    class GetPeakHours {

        @Test
        @DisplayName("should return peak hours sorted by booking count")
        void shouldReturnPeakHoursSortedByBookingCount() throws Exception {
            PeakHoursDTO peak = PeakHoursDTO.builder()
                    .timeSlotId(1L)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .bookingCount(10L)
                    .build();

            when(analyticsService.getPeakHours(TEST_SEMESTER))
                    .thenReturn(List.of(peak));

            mockMvc.perform(get("/api/analytics/peak-hours")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].timeSlotId").value(1))
                    .andExpect(jsonPath("$[0].bookingCount").value(10));
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/underused")
    class GetUnderusedRooms {

        @Test
        @DisplayName("should return underused rooms below threshold")
        void shouldReturnUnderusedRoomsBelowThreshold() throws Exception {
            RoomUtilizationDTO room = RoomUtilizationDTO.builder()
                    .roomId(1L)
                    .roomNumber("101")
                    .utilizationPercentage(20.0)
                    .build();

            when(analyticsService.getUnderusedRooms(TEST_SEMESTER, 50.0))
                    .thenReturn(List.of(room));

            mockMvc.perform(get("/api/analytics/underused")
                    .param("semester", TEST_SEMESTER)
                    .param("threshold", "50.0")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].utilizationPercentage").value(20.0));
        }

        @Test
        @DisplayName("should use default threshold when not specified")
        void shouldUseDefaultThresholdWhenNotSpecified() throws Exception {
            when(analyticsService.getUnderusedRooms(TEST_SEMESTER, 30.0))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/analytics/underused")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/summary")
    class GetUtilizationSummary {

        @Test
        @DisplayName("should return utilization summary")
        void shouldReturnUtilizationSummary() throws Exception {
            UtilizationSummaryDTO summary = UtilizationSummaryDTO.builder()
                    .semester(TEST_SEMESTER)
                    .totalRooms(10)
                    .totalBuildings(3)
                    .overallUtilizationPercentage(65.0)
                    .build();

            when(analyticsService.getUtilizationSummary(TEST_SEMESTER))
                    .thenReturn(summary);

            mockMvc.perform(get("/api/analytics/summary")
                    .param("semester", TEST_SEMESTER)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.semester").value(TEST_SEMESTER))
                    .andExpect(jsonPath("$.totalRooms").value(10))
                    .andExpect(jsonPath("$.overallUtilizationPercentage").value(65.0));
        }
    }
}
