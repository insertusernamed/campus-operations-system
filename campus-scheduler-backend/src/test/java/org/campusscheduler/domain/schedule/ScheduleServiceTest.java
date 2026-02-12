package org.campusscheduler.domain.schedule;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScheduleService.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private org.campusscheduler.domain.course.CourseRepository courseRepository;

    @Mock
    private org.campusscheduler.domain.room.RoomRepository roomRepository;

    @Mock
    private org.campusscheduler.domain.timeslot.TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private Schedule testSchedule;
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
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all schedules")
        void shouldReturnAllSchedules() {
            when(scheduleRepository.findAll()).thenReturn(List.of(testSchedule));

            List<Schedule> result = scheduleService.findAll();

            assertThat(result).hasSize(1);
            verify(scheduleRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no schedules exist")
        void shouldReturnEmptyListWhenNoSchedulesExist() {
            when(scheduleRepository.findAll()).thenReturn(List.of());

            List<Schedule> result = scheduleService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return schedule when found")
        void shouldReturnScheduleWhenFound() {
            when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

            Optional<Schedule> result = scheduleService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getCourse().getCode()).isEqualTo("CS101");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Schedule> result = scheduleService.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRoomId")
    class FindByRoomId {

        @Test
        @DisplayName("should return schedules for room")
        void shouldReturnSchedulesForRoom() {
            when(scheduleRepository.findByRoomId(1L)).thenReturn(List.of(testSchedule));

            List<Schedule> result = scheduleService.findByRoomId(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByCourseId")
    class FindByCourseId {

        @Test
        @DisplayName("should return schedules for course")
        void shouldReturnSchedulesForCourse() {
            when(scheduleRepository.findByCourseId(1L)).thenReturn(List.of(testSchedule));

            List<Schedule> result = scheduleService.findByCourseId(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByTimeSlotId")
    class FindByTimeSlotId {

        @Test
        @DisplayName("should return schedules for time slot")
        void shouldReturnSchedulesForTimeSlot() {
            when(scheduleRepository.findByTimeSlotId(1L)).thenReturn(List.of(testSchedule));

            List<Schedule> result = scheduleService.findByTimeSlotId(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create schedule when no conflicts")
        void shouldCreateScheduleWhenNoConflicts() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(1L, 1L, "Spring 2026")).thenReturn(List.of());
            when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

            Optional<Schedule> result = scheduleService.create(1L, 1L, 1L, "Spring 2026");

            assertThat(result).isPresent();
            verify(scheduleRepository).save(any(Schedule.class));
        }

        @Test
        @DisplayName("should throw exception when room has conflict")
        void shouldThrowExceptionWhenRoomHasConflict() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(1L, 1L, "Spring 2026"))
                    .thenReturn(List.of(testSchedule));

            assertThatThrownBy(() -> scheduleService.create(1L, 1L, 1L, "Spring 2026"))
                    .isInstanceOf(ScheduleConflictException.class)
                    .hasMessageContaining("Room");
        }

        @Test
        @DisplayName("should return empty when course not found")
        void shouldReturnEmptyWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Schedule> result = scheduleService.create(999L, 1L, 1L, "Spring 2026");

            assertThat(result).isEmpty();
            verify(scheduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return empty when room not found")
        void shouldReturnEmptyWhenRoomNotFound() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Schedule> result = scheduleService.create(1L, 999L, 1L, "Spring 2026");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when room capacity insufficient")
        void shouldThrowExceptionWhenRoomCapacityInsufficient() {
            Room smallRoom = Room.builder()
                    .id(2L)
                    .roomNumber("102")
                    .capacity(10) // Less than course enrollment of 30
                    .type(Room.RoomType.SEMINAR)
                    .build();

            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(roomRepository.findById(2L)).thenReturn(Optional.of(smallRoom));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));

            assertThatThrownBy(() -> scheduleService.create(1L, 2L, 1L, "Spring 2026"))
                    .isInstanceOf(ScheduleConflictException.class)
                    .hasMessageContaining("capacity");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should return true when schedule deleted")
        void shouldReturnTrueWhenScheduleDeleted() {
            when(scheduleRepository.existsById(1L)).thenReturn(true);

            boolean result = scheduleService.delete(1L);

            assertThat(result).isTrue();
            verify(scheduleRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should return false when schedule not found")
        void shouldReturnFalseWhenScheduleNotFound() {
            when(scheduleRepository.existsById(999L)).thenReturn(false);

            boolean result = scheduleService.delete(999L);

            assertThat(result).isFalse();
            verify(scheduleRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("hasConflict")
    class HasConflict {

        @Test
        @DisplayName("should return true when room is already booked for time slot")
        void shouldReturnTrueWhenRoomIsAlreadyBooked() {
            when(scheduleRepository.findByRoomIdAndTimeSlotId(1L, 1L)).thenReturn(List.of(testSchedule));

            boolean result = scheduleService.hasRoomConflict(1L, 1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when room is available")
        void shouldReturnFalseWhenRoomIsAvailable() {
            when(scheduleRepository.findByRoomIdAndTimeSlotId(1L, 1L)).thenReturn(List.of());

            boolean result = scheduleService.hasRoomConflict(1L, 1L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when room is booked in same semester")
        void shouldReturnTrueWhenRoomIsBookedInSameSemester() {
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(1L, 1L, "Spring 2026"))
                    .thenReturn(List.of(testSchedule));

            boolean result = scheduleService.hasRoomConflict(1L, 1L, "Spring 2026");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when room is free in requested semester")
        void shouldReturnFalseWhenRoomIsFreeInRequestedSemester() {
            when(scheduleRepository.findByRoomIdAndTimeSlotIdAndSemester(1L, 1L, "Fall 2026"))
                    .thenReturn(List.of());

            boolean result = scheduleService.hasRoomConflict(1L, 1L, "Fall 2026");

            assertThat(result).isFalse();
        }
    }
}
