package org.campusscheduler.domain.changerequest;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ScheduleChangeRequest entity database operations.
 */
@Repository
public interface ScheduleChangeRequestRepository extends JpaRepository<ScheduleChangeRequest, Long> {

    @EntityGraph(attributePaths = {
            "schedule",
            "schedule.course",
            "schedule.course.instructor",
            "schedule.room",
            "schedule.room.building",
            "schedule.timeSlot",
            "requestedByInstructor",
            "proposedRoom",
            "proposedRoom.building",
            "proposedTimeSlot"
    })
    @Query("""
            SELECT r FROM ScheduleChangeRequest r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:instructorId IS NULL OR r.requestedByInstructor.id = :instructorId)
              AND (:semester IS NULL OR r.originalSemester = :semester)
              AND (:scheduleId IS NULL OR r.schedule.id = :scheduleId)
            """)
    List<ScheduleChangeRequest> findByFilters(
            @Param("status") ChangeRequestStatus status,
            @Param("instructorId") Long instructorId,
            @Param("semester") String semester,
            @Param("scheduleId") Long scheduleId);
}
