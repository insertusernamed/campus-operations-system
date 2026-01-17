package org.campusscheduler.domain.timeslot;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TimeSlotService.
 */
@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
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
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all time slots")
        void shouldReturnAllTimeSlots() {
            TimeSlot slot2 = TimeSlot.builder()
                    .id(2L)
                    .dayOfWeek(DayOfWeek.TUESDAY)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 30))
                    .build();

            when(timeSlotRepository.findAll()).thenReturn(List.of(testTimeSlot, slot2));

            List<TimeSlot> result = timeSlotService.findAll();

            assertThat(result).hasSize(2);
            verify(timeSlotRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no time slots exist")
        void shouldReturnEmptyListWhenNoTimeSlotsExist() {
            when(timeSlotRepository.findAll()).thenReturn(List.of());

            List<TimeSlot> result = timeSlotService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return time slot when found")
        void shouldReturnTimeSlotWhenFound() {
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));

            Optional<TimeSlot> result = timeSlotService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(timeSlotRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<TimeSlot> result = timeSlotService.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDayOfWeek")
    class FindByDayOfWeek {

        @Test
        @DisplayName("should return time slots for day")
        void shouldReturnTimeSlotsForDay() {
            when(timeSlotRepository.findByDayOfWeekOrderByStartTime(DayOfWeek.MONDAY))
                    .thenReturn(List.of(testTimeSlot));

            List<TimeSlot> result = timeSlotService.findByDayOfWeek(DayOfWeek.MONDAY);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("should return empty list when no slots for day")
        void shouldReturnEmptyListWhenNoSlotsForDay() {
            when(timeSlotRepository.findByDayOfWeekOrderByStartTime(DayOfWeek.SATURDAY))
                    .thenReturn(List.of());

            List<TimeSlot> result = timeSlotService.findByDayOfWeek(DayOfWeek.SATURDAY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create time slot")
        void shouldCreateTimeSlot() {
            TimeSlot newSlot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 30))
                    .label("Mid-Morning")
                    .build();

            when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(i -> {
                TimeSlot saved = i.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            TimeSlot result = timeSlotService.create(newSlot);

            assertThat(result.getId()).isEqualTo(2L);
            verify(timeSlotRepository).save(newSlot);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update time slot when found")
        void shouldUpdateTimeSlotWhenFound() {
            TimeSlot updated = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 30))
                    .endTime(LocalTime.of(11, 0))
                    .label("Updated Morning")
                    .build();

            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(testTimeSlot));
            when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(i -> i.getArgument(0));

            Optional<TimeSlot> result = timeSlotService.update(1L, updated);

            assertThat(result).isPresent();
            assertThat(result.get().getStartTime()).isEqualTo(LocalTime.of(9, 30));
            assertThat(result.get().getLabel()).isEqualTo("Updated Morning");
        }

        @Test
        @DisplayName("should return empty when time slot not found")
        void shouldReturnEmptyWhenTimeSlotNotFound() {
            when(timeSlotRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<TimeSlot> result = timeSlotService.update(999L, testTimeSlot);

            assertThat(result).isEmpty();
            verify(timeSlotRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should return true when time slot is deleted")
        void shouldReturnTrueWhenTimeSlotIsDeleted() {
            when(timeSlotRepository.existsById(1L)).thenReturn(true);

            boolean result = timeSlotService.delete(1L);

            assertThat(result).isTrue();
            verify(timeSlotRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should return false when time slot not found")
        void shouldReturnFalseWhenTimeSlotNotFound() {
            when(timeSlotRepository.existsById(999L)).thenReturn(false);

            boolean result = timeSlotService.delete(999L);

            assertThat(result).isFalse();
            verify(timeSlotRepository, never()).deleteById(any());
        }
    }
}
