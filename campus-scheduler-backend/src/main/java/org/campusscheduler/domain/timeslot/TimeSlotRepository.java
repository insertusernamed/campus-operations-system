package org.campusscheduler.domain.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Repository for TimeSlot entity database operations.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * Find time slots by day of week.
     *
     * @param dayOfWeek the day of week
     * @return list of time slots on that day
     */
    List<TimeSlot> findByDayOfWeek(DayOfWeek dayOfWeek);

    /**
     * Find time slots by day of week, ordered by start time.
     *
     * @param dayOfWeek the day of week
     * @return list of time slots ordered by start time
     */
    List<TimeSlot> findByDayOfWeekOrderByStartTime(DayOfWeek dayOfWeek);
}
