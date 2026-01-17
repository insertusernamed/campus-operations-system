package org.campusscheduler.domain.timeslot;

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

import java.time.DayOfWeek;
import java.time.LocalTime;
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
 * Controller tests for TimeSlotController using MockMvc.
 */
@WebMvcTest(TimeSlotController.class)
@Import(SecurityConfig.class)
class TimeSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSlotService timeSlotService;

    private TimeSlot testTimeSlot;

    @BeforeEach
    void setUp() {
        testTimeSlot = TimeSlot.builder()
                .id(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Morning Session")
                .build();
    }

    @Nested
    @DisplayName("GET /api/timeslots")
    class GetAllTimeSlots {

        @Test
        @DisplayName("should return all time slots")
        void shouldReturnAllTimeSlots() throws Exception {
            when(timeSlotService.findAll()).thenReturn(List.of(testTimeSlot));

            mockMvc.perform(get("/api/timeslots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].dayOfWeek", is("MONDAY")))
                    .andExpect(jsonPath("$[0].label", is("Morning Session")));
        }

        @Test
        @DisplayName("should filter time slots by day of week")
        void shouldFilterTimeSlotsByDayOfWeek() throws Exception {
            when(timeSlotService.findByDayOfWeek(DayOfWeek.MONDAY))
                    .thenReturn(List.of(testTimeSlot));

            mockMvc.perform(get("/api/timeslots").param("dayOfWeek", "MONDAY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].dayOfWeek", is("MONDAY")));
        }
    }

    @Nested
    @DisplayName("GET /api/timeslots/{id}")
    class GetTimeSlotById {

        @Test
        @DisplayName("should return time slot when found")
        void shouldReturnTimeSlotWhenFound() throws Exception {
            when(timeSlotService.findById(1L)).thenReturn(Optional.of(testTimeSlot));

            mockMvc.perform(get("/api/timeslots/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.dayOfWeek", is("MONDAY")));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(timeSlotService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/timeslots/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/timeslots")
    class CreateTimeSlot {

        @Test
        @DisplayName("should create time slot and return 201")
        void shouldCreateTimeSlotAndReturn201() throws Exception {
            TimeSlot savedSlot = TimeSlot.builder()
                    .id(2L)
                    .dayOfWeek(DayOfWeek.TUESDAY)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 30))
                    .label("Afternoon")
                    .build();

            when(timeSlotService.create(any(TimeSlot.class))).thenReturn(savedSlot);

            mockMvc.perform(post("/api/timeslots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"14:00\",\"endTime\":\"15:30\",\"label\":\"Afternoon\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.dayOfWeek", is("TUESDAY")));
        }

        @Test
        @DisplayName("should return 400 when dayOfWeek is missing")
        void shouldReturn400WhenDayOfWeekIsMissing() throws Exception {
            mockMvc.perform(post("/api/timeslots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"startTime\":\"09:00\",\"endTime\":\"10:30\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when startTime is missing")
        void shouldReturn400WhenStartTimeIsMissing() throws Exception {
            mockMvc.perform(post("/api/timeslots")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"dayOfWeek\":\"MONDAY\",\"endTime\":\"10:30\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/timeslots/{id}")
    class UpdateTimeSlot {

        @Test
        @DisplayName("should update time slot when found")
        void shouldUpdateTimeSlotWhenFound() throws Exception {
            TimeSlot updated = TimeSlot.builder()
                    .id(1L)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 30))
                    .endTime(LocalTime.of(11, 0))
                    .label("Extended Morning")
                    .build();

            when(timeSlotService.update(eq(1L), any(TimeSlot.class))).thenReturn(Optional.of(updated));

            mockMvc.perform(put("/api/timeslots/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                            "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:30\",\"endTime\":\"11:00\",\"label\":\"Extended Morning\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.label", is("Extended Morning")));
        }

        @Test
        @DisplayName("should return 404 when time slot not found")
        void shouldReturn404WhenTimeSlotNotFound() throws Exception {
            when(timeSlotService.update(eq(999L), any(TimeSlot.class))).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/timeslots/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"10:30\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/timeslots/{id}")
    class DeleteTimeSlot {

        @Test
        @DisplayName("should return 204 when deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            when(timeSlotService.delete(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/timeslots/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(timeSlotService.delete(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/timeslots/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
