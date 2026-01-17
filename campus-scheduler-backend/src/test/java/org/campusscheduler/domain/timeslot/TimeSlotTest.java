package org.campusscheduler.domain.timeslot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TimeSlot entity methods.
 */
class TimeSlotTest {

    @Nested
    @DisplayName("overlapsWith")
    class OverlapsWith {

        @Test
        @DisplayName("should return true for overlapping slots on same day")
        void shouldReturnTrueForOverlappingSlotsOnSameDay() {
            TimeSlot slot1 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .build();

            TimeSlot slot2 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 30))
                    .build();

            assertThat(slot1.overlapsWith(slot2)).isTrue();
            assertThat(slot2.overlapsWith(slot1)).isTrue();
        }

        @Test
        @DisplayName("should return false for adjacent slots (no overlap)")
        void shouldReturnFalseForAdjacentSlots() {
            TimeSlot slot1 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 0))
                    .build();

            TimeSlot slot2 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .build();

            assertThat(slot1.overlapsWith(slot2)).isFalse();
            assertThat(slot2.overlapsWith(slot1)).isFalse();
        }

        @Test
        @DisplayName("should return false for slots on different days")
        void shouldReturnFalseForSlotsOnDifferentDays() {
            TimeSlot slot1 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .build();

            TimeSlot slot2 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.TUESDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .build();

            assertThat(slot1.overlapsWith(slot2)).isFalse();
        }

        @Test
        @DisplayName("should return true when one slot contains another")
        void shouldReturnTrueWhenOneSlotContainsAnother() {
            TimeSlot outer = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(12, 0))
                    .build();

            TimeSlot inner = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(11, 0))
                    .build();

            assertThat(outer.overlapsWith(inner)).isTrue();
            assertThat(inner.overlapsWith(outer)).isTrue();
        }

        @Test
        @DisplayName("should return true for exact match")
        void shouldReturnTrueForExactMatch() {
            TimeSlot slot1 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.FRIDAY)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 30))
                    .build();

            TimeSlot slot2 = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.FRIDAY)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 30))
                    .build();

            assertThat(slot1.overlapsWith(slot2)).isTrue();
        }
    }

    @Nested
    @DisplayName("getDurationMinutes")
    class GetDurationMinutes {

        @Test
        @DisplayName("should return correct duration for 90-minute slot")
        void shouldReturnCorrectDurationFor90MinuteSlot() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .build();

            assertThat(slot.getDurationMinutes()).isEqualTo(90);
        }

        @Test
        @DisplayName("should return correct duration for 60-minute slot")
        void shouldReturnCorrectDurationFor60MinuteSlot() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.TUESDAY)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 0))
                    .build();

            assertThat(slot.getDurationMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("should return correct duration for 3-hour slot")
        void shouldReturnCorrectDurationFor3HourSlot() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.THURSDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(12, 0))
                    .build();

            assertThat(slot.getDurationMinutes()).isEqualTo(180);
        }
    }

    @Nested
    @DisplayName("isValidTimeRange")
    class IsValidTimeRange {

        @Test
        @DisplayName("should return true when startTime is before endTime")
        void shouldReturnTrueWhenStartTimeBeforeEndTime() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .build();

            assertThat(slot.isValidTimeRange()).isTrue();
        }

        @Test
        @DisplayName("should return false when startTime equals endTime")
        void shouldReturnFalseWhenStartTimeEqualsEndTime() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build();

            assertThat(slot.isValidTimeRange()).isFalse();
        }

        @Test
        @DisplayName("should return false when startTime is after endTime")
        void shouldReturnFalseWhenStartTimeAfterEndTime() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(11, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build();

            assertThat(slot.isValidTimeRange()).isFalse();
        }

        @Test
        @DisplayName("should return true when startTime is null")
        void shouldReturnTrueWhenStartTimeIsNull() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(null)
                    .endTime(LocalTime.of(10, 0))
                    .build();

            assertThat(slot.isValidTimeRange()).isTrue();
        }

        @Test
        @DisplayName("should return true when endTime is null")
        void shouldReturnTrueWhenEndTimeIsNull() {
            TimeSlot slot = TimeSlot.builder()
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(null)
                    .build();

            assertThat(slot.isValidTimeRange()).isTrue();
        }
    }
}
