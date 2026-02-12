package org.campusscheduler.domain.changerequest;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.instructor.Instructor;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleChangeRequestController.class)
@Import(SecurityConfig.class)
class ScheduleChangeRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleChangeRequestService changeRequestService;

    private ScheduleChangeRequest testRequest;

    @BeforeEach
    void setUp() {
        Instructor instructor = Instructor.builder()
                .id(10L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@campus.edu")
                .department("Computer Science")
                .officeNumber("CS-201")
                .build();

        Course course = Course.builder()
                .id(20L)
                .code("CS101")
                .name("Intro to Programming")
                .enrollmentCapacity(30)
                .instructor(instructor)
                .build();

        Room room = Room.builder()
                .id(30L)
                .roomNumber("101")
                .capacity(40)
                .type(Room.RoomType.CLASSROOM)
                .build();

        Room proposedRoom = Room.builder()
                .id(31L)
                .roomNumber("202")
                .capacity(35)
                .type(Room.RoomType.CLASSROOM)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(40L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .label("Morning")
                .build();

        TimeSlot proposedTimeSlot = TimeSlot.builder()
                .id(41L)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .label("Late Morning")
                .build();

        Schedule schedule = Schedule.builder()
                .id(50L)
                .course(course)
                .room(room)
                .timeSlot(timeSlot)
                .semester("Fall 2026")
                .build();

        testRequest = ScheduleChangeRequest.builder()
                .id(60L)
                .schedule(schedule)
                .requestedByInstructor(instructor)
                .requestedByRole(ChangeRequestRole.INSTRUCTOR)
                .status(ChangeRequestStatus.PENDING)
                .reasonCategory(ChangeRequestReason.MEDICAL)
                .reasonDetails("Medical appointment")
                .proposedRoom(proposedRoom)
                .proposedTimeSlot(proposedTimeSlot)
                .originalRoomId(room.getId())
                .originalTimeSlotId(timeSlot.getId())
                .originalSemester("Fall 2026")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/change-requests")
    class GetAll {

        @Test
        @DisplayName("should return change requests")
        void shouldReturnChangeRequests() throws Exception {
            when(changeRequestService.findAll(null, null, null, null))
                    .thenReturn(List.of(testRequest));

            mockMvc.perform(get("/api/change-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(60)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")))
                    .andExpect(jsonPath("$[0].reasonCategory", is("MEDICAL")));
        }
    }

    @Nested
    @DisplayName("POST /api/change-requests")
    class Create {

        @Test
        @DisplayName("should create change request")
        void shouldCreateChangeRequest() throws Exception {
            when(changeRequestService.create(any(ChangeRequestCreateRequest.class)))
                    .thenReturn(Optional.of(testRequest));

            mockMvc.perform(post("/api/change-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "scheduleId": 50,
                              "requestedByInstructorId": 10,
                              "requestedByRole": "INSTRUCTOR",
                              "reasonCategory": "MEDICAL",
                              "reasonDetails": "Medical appointment",
                              "proposedTimeSlotId": 41
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(60)))
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }
    }

    @Nested
    @DisplayName("POST /api/change-requests/{id}/approve")
    class Approve {

        @Test
        @DisplayName("should return conflict payload when approval has hard conflicts")
        void shouldReturnConflictPayloadWhenApprovalHasHardConflicts() throws Exception {
            when(changeRequestService.approve(any(), any()))
                    .thenThrow(new ChangeRequestConflictException(
                            "Change request conflicts with existing schedules",
                            List.of("Room conflict with CS200 at this time")));

            mockMvc.perform(post("/api/change-requests/60/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "decisionNote": "Needs manual review"
                            }
                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("conflicts")))
                    .andExpect(jsonPath("$.hardConflicts[0]",
                            containsString("Room conflict")));
        }
    }

    @Nested
    @DisplayName("POST /api/change-requests/validate")
    class Validate {

        @Test
        @DisplayName("should return 404 when schedule does not exist")
        void shouldReturn404WhenScheduleDoesNotExist() throws Exception {
            when(changeRequestService.validate(any(ChangeRequestValidationRequest.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/change-requests/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "scheduleId": 999,
                              "proposedRoomId": 31
                            }
                            """))
                    .andExpect(status().isNotFound());
        }
    }
}
