package org.campusscheduler.domain.schedule;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for ScheduleController using MockMvc.
 */
@WebMvcTest(ScheduleController.class)
@Import(SecurityConfig.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private ScheduleResponseService scheduleResponseService;

    private Schedule testSchedule;
    private ScheduleResponse testScheduleResponse;
    private Course testCourse;
    private Room testRoom;
    private TimeSlot testTimeSlot;

    @BeforeEach
    void setUp() {
        testCourse = Course.builder()
                .id(1L)
                .code("CS101")
                .name("Intro to Programming")
                .credits(3)
                .enrollmentCapacity(30)
                .build();

        testRoom = Room.builder()
                .id(1L)
                .roomNumber("101")
                .capacity(35)
                .type(Room.RoomType.CLASSROOM)
                .build();

        testTimeSlot = TimeSlot.builder()
                .id(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Period 1")
                .build();

        testSchedule = Schedule.builder()
                .id(1L)
                .course(testCourse)
                .room(testRoom)
                .timeSlot(testTimeSlot)
                .semester("Spring 2026")
                .build();

        testScheduleResponse = new ScheduleResponse(
                1L,
                ScheduleResponse.CourseSummary.from(testCourse),
                ScheduleResponse.RoomSummary.from(testRoom),
                ScheduleResponse.TimeSlotSummary.from(testTimeSlot),
                "Spring 2026",
                20,
                30,
                10,
                3);
    }

    @Nested
    @DisplayName("GET /api/schedules")
    class GetAllSchedules {

        @Test
        @DisplayName("should return all schedules")
        void shouldReturnAllSchedules() throws Exception {
            when(scheduleService.findAll()).thenReturn(List.of(testSchedule));
            when(scheduleResponseService.toResponses(List.of(testSchedule))).thenReturn(List.of(testScheduleResponse));

            mockMvc.perform(get("/api/schedules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].semester", is("Spring 2026")))
                    .andExpect(jsonPath("$[0].filledSeats", is(20)))
                    .andExpect(jsonPath("$[0].waitlistCount", is(3)));
        }

        @Test
        @DisplayName("should filter schedules by roomId")
        void shouldFilterSchedulesByRoomId() throws Exception {
            when(scheduleService.findByRoomId(1L)).thenReturn(List.of(testSchedule));
            when(scheduleResponseService.toResponses(List.of(testSchedule))).thenReturn(List.of(testScheduleResponse));

            mockMvc.perform(get("/api/schedules").param("roomId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("should filter schedules by courseId")
        void shouldFilterSchedulesByCourseId() throws Exception {
            when(scheduleService.findByCourseId(1L)).thenReturn(List.of(testSchedule));
            when(scheduleResponseService.toResponses(List.of(testSchedule))).thenReturn(List.of(testScheduleResponse));

            mockMvc.perform(get("/api/schedules").param("courseId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("should filter schedules by semester")
        void shouldFilterSchedulesBySemester() throws Exception {
            when(scheduleService.findBySemester("Spring 2026")).thenReturn(List.of(testSchedule));
            when(scheduleResponseService.toResponses(List.of(testSchedule))).thenReturn(List.of(testScheduleResponse));

            mockMvc.perform(get("/api/schedules").param("semester", "Spring 2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/schedules/{id}")
    class GetScheduleById {

        @Test
        @DisplayName("should return schedule when found")
        void shouldReturnScheduleWhenFound() throws Exception {
            when(scheduleService.findById(1L)).thenReturn(Optional.of(testSchedule));
            when(scheduleResponseService.toResponse(testSchedule)).thenReturn(testScheduleResponse);

            mockMvc.perform(get("/api/schedules/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.semester", is("Spring 2026")))
                    .andExpect(jsonPath("$.seatLimit", is(30)));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(scheduleService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/schedules/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/schedules")
    class CreateSchedule {

        @Test
        @DisplayName("should create schedule and return 201")
        void shouldCreateScheduleAndReturn201() throws Exception {
            when(scheduleService.create(anyLong(), anyLong(), anyLong(), anyString()))
                    .thenReturn(Optional.of(testSchedule));
            when(scheduleResponseService.toResponse(testSchedule)).thenReturn(testScheduleResponse);

            mockMvc.perform(post("/api/schedules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"courseId\":1,\"roomId\":1,\"timeSlotId\":1,\"semester\":\"Spring 2026\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.remainingSeats", is(10)));
        }

        @Test
        @DisplayName("should return 404 when course/room/timeslot not found")
        void shouldReturn404WhenEntityNotFound() throws Exception {
            when(scheduleService.create(anyLong(), anyLong(), anyLong(), anyString()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/schedules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"courseId\":999,\"roomId\":1,\"timeSlotId\":1,\"semester\":\"Spring 2026\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when conflict exists")
        void shouldReturn409WhenConflictExists() throws Exception {
            when(scheduleService.create(anyLong(), anyLong(), anyLong(), anyString()))
                    .thenThrow(new ScheduleConflictException("Room is already booked"));

            mockMvc.perform(post("/api/schedules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"courseId\":1,\"roomId\":1,\"timeSlotId\":1,\"semester\":\"Spring 2026\"}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/schedules/{id}")
    class DeleteSchedule {

        @Test
        @DisplayName("should return 204 when deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            when(scheduleService.delete(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/schedules/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(scheduleService.delete(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/schedules/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/schedules/conflicts")
    class CheckConflicts {

        @Test
        @DisplayName("should return conflict status for room and time slot")
        void shouldReturnConflictStatusForRoomAndTimeSlot() throws Exception {
            when(scheduleService.hasRoomConflict(1L, 1L)).thenReturn(true);

            mockMvc.perform(get("/api/schedules/conflicts")
                    .param("roomId", "1")
                    .param("timeSlotId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConflict", is(true)));
        }

        @Test
        @DisplayName("should return no conflict when room is free")
        void shouldReturnNoConflictWhenRoomIsFree() throws Exception {
            when(scheduleService.hasRoomConflict(1L, 2L)).thenReturn(false);

            mockMvc.perform(get("/api/schedules/conflicts")
                    .param("roomId", "1")
                    .param("timeSlotId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConflict", is(false)));
        }

        @Test
        @DisplayName("should check conflicts within a specific semester")
        void shouldCheckConflictsWithinSpecificSemester() throws Exception {
            when(scheduleService.hasRoomConflict(1L, 1L, "Fall 2026")).thenReturn(true);

            mockMvc.perform(get("/api/schedules/conflicts")
                    .param("roomId", "1")
                    .param("timeSlotId", "1")
                    .param("semester", "Fall 2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConflict", is(true)));
        }
    }
}
