package org.campusscheduler.domain.roombooking;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.room.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomBookingController.class)
@Import(SecurityConfig.class)
class RoomBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomBookingService roomBookingService;

    private RoomBookingResponse sampleResponse() {
        return new RoomBookingResponse(
                77L,
                org.campusscheduler.domain.room.RoomResponse.builder()
                        .id(10L)
                        .roomNumber("201")
                        .capacity(20)
                        .type(Room.RoomType.SEMINAR)
                        .availabilityStatus(Room.AvailabilityStatus.AVAILABLE)
                        .featureSet(List.of())
                        .accessibilityFlags(List.of())
                        .lastInspectionDate((LocalDate) null)
                        .buildingId(30L)
                        .buildingCode("ENG")
                        .buildingName("Engineering")
                        .build(),
                new org.campusscheduler.domain.schedule.ScheduleResponse.TimeSlotSummary(
                        20L,
                        java.time.DayOfWeek.MONDAY,
                        java.time.LocalTime.of(10, 0),
                        java.time.LocalTime.of(11, 15),
                        "Mon 10:00"),
                "Fall 2026",
                Instant.parse("2026-03-30T10:00:00Z"),
                2,
                true,
                true,
                true,
                new RoomBookingParticipantResponse(1L, "Maya Patel", "maya.patel@students.campus.edu"),
                List.of(new RoomBookingParticipantResponse(2L, "Jonah Lee", "jonah.lee@students.campus.edu")));
    }

    @Nested
    @DisplayName("GET /api/room-bookings")
    class GetAll {

        @Test
        @DisplayName("should return visible room bookings")
        void shouldReturnVisibleRoomBookings() throws Exception {
            when(roomBookingService.findVisibleBookings("Fall 2026", null, "admin", 1L))
                    .thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/room-bookings")
                    .param("semester", "Fall 2026")
                    .header("X-Viewer-Role", "admin")
                    .header("X-Viewer-Student-Id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].room.roomNumber", is("201")))
                    .andExpect(jsonPath("$[0].bookedBy.fullName", is("Maya Patel")));
        }
    }

    @Nested
    @DisplayName("POST /api/room-bookings")
    class Create {

        @Test
        @DisplayName("should create booking when request is valid")
        void shouldCreateBooking() throws Exception {
            when(roomBookingService.create(org.mockito.ArgumentMatchers.any(CreateRoomBookingRequest.class), eq("student"), eq(1L)))
                    .thenReturn(Optional.of(sampleResponse()));

            mockMvc.perform(post("/api/room-bookings")
                    .contentType(APPLICATION_JSON)
                    .header("X-Viewer-Role", "student")
                    .header("X-Viewer-Student-Id", "1")
                    .content("""
                            {
                              "studentId": 1,
                              "roomId": 10,
                              "timeSlotId": 20,
                              "semester": "Fall 2026",
                              "participantEmails": ["jonah.lee@students.campus.edu"]
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(77)))
                    .andExpect(jsonPath("$.participantCount", is(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/room-bookings/student-search")
    class StudentSearch {

        @Test
        @DisplayName("should return lookup results with class overlap info")
        void shouldReturnLookupResults() throws Exception {
            when(roomBookingService.searchStudentsByEmail("jon", "Fall 2026", 20L, List.of(1L)))
                    .thenReturn(List.of(new RoomBookingStudentLookupResponse(
                            2L,
                            "jonah.lee@students.campus.edu",
                            "Jonah Lee",
                            false)));

            mockMvc.perform(get("/api/room-bookings/student-search")
                    .param("query", "jon")
                    .param("semester", "Fall 2026")
                    .param("timeSlotId", "20")
                    .param("excludeStudentId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fullName", is("Jonah Lee")))
                    .andExpect(jsonPath("$[0].hasClassDuringPeriod", is(false)));
        }
    }
}
