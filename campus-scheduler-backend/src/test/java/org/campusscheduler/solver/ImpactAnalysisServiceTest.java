package org.campusscheduler.solver;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ImpactAnalysisService.
 */
@ExtendWith(MockitoExtension.class)
class ImpactAnalysisServiceTest {

    @Mock
    private SolverManagerConfiguration solverConfig;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private ImpactAnalysisService impactAnalysisService;

    @Test
    void analyzeRequiresProposedChange() {
        ImpactAnalysisRequest request = new ImpactAnalysisRequest();
        request.setScheduleId(1L);

        assertThatThrownBy(() -> impactAnalysisService.analyze(request))
                .isInstanceOf(ImpactAnalysisStateException.class);
    }

    @Test
    void analyzeRejectsMissingScheduleAssignments() {
        ImpactAnalysisRequest request = new ImpactAnalysisRequest();
        request.setScheduleId(1L);
        request.setProposedRoomId(2L);

        Schedule schedule = Schedule.builder()
                .id(1L)
                .course(Course.builder().id(1L).code("CS101").enrollmentCapacity(20)
                        .instructor(Instructor.builder().id(10L).build())
                        .build())
                .semester("Fall 2026")
                .build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(Room.builder().id(2L).roomNumber("101")
                .capacity(30).type(Room.RoomType.CLASSROOM).build()));

        assertThatThrownBy(() -> impactAnalysisService.analyze(request))
                .isInstanceOf(ImpactAnalysisStateException.class);
    }

    @Test
    void analyzeReturnsEmptyWhenScheduleMissing() {
        ImpactAnalysisRequest request = new ImpactAnalysisRequest();
        request.setScheduleId(1L);
        request.setProposedRoomId(2L);

        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(impactAnalysisService.analyze(request)).isEmpty();
    }

    @Test
    void analyzeReturnsEmptyWhenRoomMissing() {
        ImpactAnalysisRequest request = new ImpactAnalysisRequest();
        request.setScheduleId(1L);
        request.setProposedRoomId(2L);

        Schedule schedule = buildSchedule();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(roomRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(impactAnalysisService.analyze(request)).isEmpty();
    }

    @Test
    void analyzeReturnsEmptyWhenTimeSlotMissing() {
        ImpactAnalysisRequest request = new ImpactAnalysisRequest();
        request.setScheduleId(1L);
        request.setProposedTimeSlotId(2L);

        Schedule schedule = buildSchedule();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(timeSlotRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(impactAnalysisService.analyze(request)).isEmpty();
    }

    private Schedule buildSchedule() {
        return Schedule.builder()
                .id(1L)
                .course(Course.builder().id(1L).code("CS101").enrollmentCapacity(20)
                        .instructor(Instructor.builder().id(10L).build())
                        .build())
                .room(Room.builder().id(1L).roomNumber("100")
                        .capacity(30).type(Room.RoomType.CLASSROOM).build())
                .timeSlot(TimeSlot.builder().id(1L)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(10, 0))
                        .label("Morning")
                        .build())
                .semester("Fall 2026")
                .build();
    }
}
