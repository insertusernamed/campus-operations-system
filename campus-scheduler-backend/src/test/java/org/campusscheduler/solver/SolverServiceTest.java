package org.campusscheduler.solver;

import ai.timefold.solver.core.api.solver.SolverStatus;
import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SolverServiceTest {

    @Mock
    private SolverManagerConfiguration solverConfig;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SolverService solverService;

    @Test
    void getAnalyticsHandlesVolatileAssignmentVariables() {
        Building building = Building.builder()
                .id(1L)
                .name("Science Building")
                .code("SCI")
                .build();

        Room room = Room.builder()
                .id(11L)
                .roomNumber("101")
                .capacity(30)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(21L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 15))
                .label("Period 1")
                .build();

        ScheduleAssignment assignment = new FlakyTimeSlotAssignment(room, timeSlot, "Winter 2026");
        ScheduleSolution solution = ScheduleSolution.builder()
                .semester("Winter 2026")
                .rooms(List.of(room))
                .timeSlots(List.of(timeSlot))
                .assignments(List.of(assignment))
                .build();

        atomicRef("bestSolution").set(solution);
        atomicRef("solverStatus").set(SolverStatus.SOLVING_ACTIVE);

        SolverService.SolverAnalyticsResponse response = solverService.getAnalytics("Winter 2026");

        assertThat(response.totalScheduledSlots()).isEqualTo(1L);
        assertThat(response.peakHours()).hasSize(1);
        assertThat(response.peakHours().getFirst().bookingCount()).isEqualTo(1L);
    }

    @SuppressWarnings("unchecked")
    private <T> AtomicReference<T> atomicRef(String fieldName) {
        return (AtomicReference<T>) ReflectionTestUtils.getField(solverService, fieldName);
    }

    /**
     * Simulates assignment variables changing between repeated getter calls while
     * analytics are read concurrently with solving.
     */
    private static final class FlakyTimeSlotAssignment extends ScheduleAssignment {
        private boolean firstRead = true;

        private FlakyTimeSlotAssignment(Room room, TimeSlot timeSlot, String semester) {
            setId(1L);
            setRoom(room);
            setTimeSlot(timeSlot);
            setSemester(semester);
        }

        @Override
        public TimeSlot getTimeSlot() {
            if (firstRead) {
                firstRead = false;
                return super.getTimeSlot();
            }
            return null;
        }
    }
}
