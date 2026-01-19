package org.campusscheduler.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Schedule entity database operations.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Find schedules by room ID.
     */
    List<Schedule> findByRoomId(Long roomId);

    /**
     * Find schedules by course ID.
     */
    List<Schedule> findByCourseId(Long courseId);

    /**
     * Find schedules by time slot ID.
     */
    List<Schedule> findByTimeSlotId(Long timeSlotId);

    /**
     * Find schedules by room and time slot (for conflict detection).
     */
    List<Schedule> findByRoomIdAndTimeSlotId(Long roomId, Long timeSlotId);

    /**
     * Find schedules by room, time slot, and semester (for semester-specific
     * conflict detection).
     */
    List<Schedule> findByRoomIdAndTimeSlotIdAndSemester(Long roomId, Long timeSlotId, String semester);

    /**
     * Find schedules by semester.
     */
    List<Schedule> findBySemester(String semester);
}
