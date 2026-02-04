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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
