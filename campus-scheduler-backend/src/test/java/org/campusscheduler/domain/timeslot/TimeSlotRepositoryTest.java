package org.campusscheduler.domain.timeslot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for TimeSlot entity.
 */
@DataJpaTest
class TimeSlotRepositoryTest {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private TimeSlot mondayMorning;
    private TimeSlot mondayAfternoon;
    private TimeSlot tuesdayMorning;

    @BeforeEach
    void setUp() {
        mondayMorning = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Morning Session")
                .build();
        timeSlotRepository.save(mondayMorning);

        mondayAfternoon = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
                .label("Afternoon Session")
                .build();
        timeSlotRepository.save(mondayAfternoon);

        tuesdayMorning = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
        timeSlotRepository.save(tuesdayMorning);
    }

    @Test
    @DisplayName("should find time slots by day of week")
    void shouldFindTimeSlotsByDayOfWeek() {
        List<TimeSlot> result = timeSlotRepository.findByDayOfWeek(DayOfWeek.MONDAY);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("should find time slots ordered by start time")
    void shouldFindTimeSlotsOrderedByStartTime() {
        List<TimeSlot> result = timeSlotRepository.findByDayOfWeekOrderByStartTime(DayOfWeek.MONDAY);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(1).getStartTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    @DisplayName("should return empty list when no slots for day")
    void shouldReturnEmptyListWhenNoSlotsForDay() {
        List<TimeSlot> result = timeSlotRepository.findByDayOfWeek(DayOfWeek.SATURDAY);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should save time slot with all fields")
    void shouldSaveTimeSlotWithAllFields() {
        TimeSlot newSlot = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 30))
                .label("Friday Afternoon")
                .build();

        TimeSlot saved = timeSlotRepository.save(newSlot);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(saved.getLabel()).isEqualTo("Friday Afternoon");
    }

    @Test
    @DisplayName("should find time slot by ID")
    void shouldFindTimeSlotById() {
        Optional<TimeSlot> result = timeSlotRepository.findById(mondayMorning.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getLabel()).isEqualTo("Morning Session");
    }

    @Test
    @DisplayName("should delete time slot")
    void shouldDeleteTimeSlot() {
        Long id = mondayMorning.getId();
        timeSlotRepository.deleteById(id);

        Optional<TimeSlot> result = timeSlotRepository.findById(id);
        assertThat(result).isEmpty();
    }
}
