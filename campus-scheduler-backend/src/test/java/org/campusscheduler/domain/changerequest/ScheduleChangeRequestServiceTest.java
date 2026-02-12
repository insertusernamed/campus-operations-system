package org.campusscheduler.domain.changerequest;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.schedule.ScheduleService;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScheduleChangeRequestService.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleChangeRequestServiceTest {

    @Mock
    private ScheduleChangeRequestRepository changeRequestRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleChangeRequestService changeRequestService;

    @Test
    void createRequiresProposedChange() {
        Schedule schedule = buildSchedule();
        Instructor instructor = Instructor.builder().id(10L).build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(instructorRepository.findById(10L)).thenReturn(Optional.of(instructor));

        ChangeRequestCreateRequest request = new ChangeRequestCreateRequest();
        request.setScheduleId(1L);
        request.setRequestedByInstructorId(10L);
        request.setRequestedByRole(ChangeRequestRole.INSTRUCTOR);
        request.setReasonCategory(ChangeRequestReason.MEDICAL);

        assertThatThrownBy(() -> changeRequestService.create(request))
                .isInstanceOf(ChangeRequestStateException.class);
    }

    @Test
    void validateDetectsRoomOverlapConflicts() {
        Schedule schedule = buildSchedule();
        TimeSlot proposed = TimeSlot.builder()
                .id(2L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .label("Proposed")
                .build();

        Schedule conflicting = Schedule.builder()
                .id(2L)
                .course(Course.builder().id(2L).code("CS200").enrollmentCapacity(25).build())
                .room(schedule.getRoom())
                .timeSlot(TimeSlot.builder()
                        .id(3L)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(9, 15))
                        .endTime(LocalTime.of(10, 15))
                        .label("Overlap")
                        .build())
                .semester(schedule.getSemester())
                .build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(timeSlotRepository.findById(2L)).thenReturn(Optional.of(proposed));
        when(scheduleRepository.findByRoomIdAndSemester(schedule.getRoom().getId(), schedule.getSemester()))
                .thenReturn(List.of(conflicting));
        when(scheduleRepository.findByCourseInstructorIdAndSemesterAndTimeSlotDayOfWeek(
                schedule.getCourse().getInstructor().getId(),
                schedule.getSemester(),
                DayOfWeek.MONDAY)).thenReturn(List.of(schedule));

        ChangeRequestValidationRequest request = new ChangeRequestValidationRequest();
        request.setScheduleId(1L);
        request.setProposedTimeSlotId(2L);

        Optional<ChangeRequestValidationResponse> response = changeRequestService.validate(request);

        assertThat(response).isPresent();
        assertThat(response.get().getHardConflicts()).isNotEmpty();
    }

    @Test
    void approveAppliesScheduleUpdateAndMarksRequestApproved() {
        Schedule schedule = buildSchedule();

        Room proposedRoom = Room.builder()
                .id(6L)
                .roomNumber("202")
                .capacity(40)
                .type(Room.RoomType.CLASSROOM)
                .build();

        TimeSlot proposedTimeSlot = TimeSlot.builder()
                .id(2L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .label("Late Morning")
                .build();

        ScheduleChangeRequest existing = ScheduleChangeRequest.builder()
                .id(100L)
                .schedule(schedule)
                .requestedByInstructor(schedule.getCourse().getInstructor())
                .requestedByRole(ChangeRequestRole.INSTRUCTOR)
                .status(ChangeRequestStatus.PENDING)
                .reasonCategory(ChangeRequestReason.MEDICAL)
                .originalRoomId(schedule.getRoom().getId())
                .originalTimeSlotId(schedule.getTimeSlot().getId())
                .originalSemester(schedule.getSemester())
                .proposedRoom(proposedRoom)
                .proposedTimeSlot(proposedTimeSlot)
                .createdAt(LocalDateTime.now())
                .build();

        when(changeRequestRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.findByRoomIdAndSemester(proposedRoom.getId(), schedule.getSemester()))
                .thenReturn(List.of(schedule));
        when(scheduleRepository.findByCourseInstructorIdAndSemesterAndTimeSlotDayOfWeek(
                schedule.getCourse().getInstructor().getId(),
                schedule.getSemester(),
                proposedTimeSlot.getDayOfWeek()))
                .thenReturn(List.of(schedule));
        when(scheduleService.updateScheduleRoomTime(
                eq(schedule.getId()),
                eq(proposedRoom.getId()),
                eq(proposedTimeSlot.getId()),
                any()))
                .thenReturn(Optional.of(schedule));
        when(changeRequestRepository.save(any(ScheduleChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeRequestDecisionRequest decisionRequest = new ChangeRequestDecisionRequest();
        decisionRequest.setDecisionNote("Approved for accessibility needs");

        Optional<ScheduleChangeRequest> response = changeRequestService.approve(100L, decisionRequest);

        assertThat(response).isPresent();
        assertThat(response.get().getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(response.get().getDecisionNote()).isEqualTo("Approved for accessibility needs");
        assertThat(response.get().getReviewedAt()).isNotNull();
        assertThat(response.get().getAppliedAt()).isNotNull();
    }

    @Test
    void rejectMarksRequestRejectedWithDecisionNote() {
        Schedule schedule = buildSchedule();
        ScheduleChangeRequest existing = ScheduleChangeRequest.builder()
                .id(101L)
                .schedule(schedule)
                .requestedByInstructor(schedule.getCourse().getInstructor())
                .requestedByRole(ChangeRequestRole.INSTRUCTOR)
                .status(ChangeRequestStatus.PENDING)
                .reasonCategory(ChangeRequestReason.OTHER)
                .originalRoomId(schedule.getRoom().getId())
                .originalTimeSlotId(schedule.getTimeSlot().getId())
                .originalSemester(schedule.getSemester())
                .createdAt(LocalDateTime.now())
                .build();

        when(changeRequestRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(changeRequestRepository.save(any(ScheduleChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeRequestDecisionRequest decisionRequest = new ChangeRequestDecisionRequest();
        decisionRequest.setDecisionNote("Rejected due to room limitations");

        Optional<ScheduleChangeRequest> response = changeRequestService.reject(101L, decisionRequest);

        assertThat(response).isPresent();
        assertThat(response.get().getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
        assertThat(response.get().getDecisionNote()).isEqualTo("Rejected due to room limitations");
        assertThat(response.get().getReviewedAt()).isNotNull();
        assertThat(response.get().getAppliedAt()).isNull();
    }

    private Schedule buildSchedule() {
        Instructor instructor = Instructor.builder().id(10L).build();
        Course course = Course.builder()
                .id(1L)
                .code("CS101")
                .enrollmentCapacity(30)
                .instructor(instructor)
                .build();
        Room room = Room.builder()
                .id(5L)
                .roomNumber("101")
                .capacity(35)
                .type(Room.RoomType.CLASSROOM)
                .build();
        TimeSlot slot = TimeSlot.builder()
                .id(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .label("Morning")
                .build();

        return Schedule.builder()
                .id(1L)
                .course(course)
                .room(room)
                .timeSlot(slot)
                .semester("Fall 2026")
                .build();
    }
}
