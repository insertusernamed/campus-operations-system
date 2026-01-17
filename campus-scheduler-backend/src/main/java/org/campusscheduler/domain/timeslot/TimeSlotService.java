package org.campusscheduler.domain.timeslot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for TimeSlot business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    /**
     * Get all time slots.
     *
     * @return list of all time slots
     */
    public List<TimeSlot> findAll() {
        return timeSlotRepository.findAll();
    }

    /**
     * Find a time slot by ID.
     *
     * @param id the time slot ID
     * @return optional containing the time slot if found
     */
    public Optional<TimeSlot> findById(Long id) {
        return timeSlotRepository.findById(id);
    }

    /**
     * Find time slots by day of week.
     *
     * @param dayOfWeek the day of week
     * @return list of time slots on that day
     */
    public List<TimeSlot> findByDayOfWeek(DayOfWeek dayOfWeek) {
        return timeSlotRepository.findByDayOfWeekOrderByStartTime(dayOfWeek);
    }

    /**
     * Create a new time slot.
     *
     * @param timeSlot the time slot to create
     * @return the created time slot
     */
    @Transactional
    public TimeSlot create(TimeSlot timeSlot) {
        return timeSlotRepository.save(timeSlot);
    }

    /**
     * Update an existing time slot.
     *
     * @param id      the time slot ID
     * @param updated the updated time slot data
     * @return optional containing the updated time slot if found
     */
    @Transactional
    public Optional<TimeSlot> update(Long id, TimeSlot updated) {
        return timeSlotRepository.findById(id)
                .map(existing -> {
                    existing.setDayOfWeek(updated.getDayOfWeek());
                    existing.setStartTime(updated.getStartTime());
                    existing.setEndTime(updated.getEndTime());
                    existing.setLabel(updated.getLabel());
                    return timeSlotRepository.save(existing);
                });
    }

    /**
     * Delete a time slot by ID.
     *
     * @param id the time slot ID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(Long id) {
        if (timeSlotRepository.existsById(id)) {
            timeSlotRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
