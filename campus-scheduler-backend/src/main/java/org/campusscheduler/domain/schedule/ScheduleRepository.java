package org.campusscheduler.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
	 * Find schedules by instructor ID.
	 */
	List<Schedule> findByCourseInstructorId(Long instructorId);

	/**
	 * Find schedules by instructor ID and semester.
	 */
	List<Schedule> findByCourseInstructorIdAndSemester(Long instructorId, String semester);

	/**
	 * Find schedules by instructor ID, time slot, and semester.
	 */
	List<Schedule> findByCourseInstructorIdAndTimeSlotIdAndSemester(Long instructorId, Long timeSlotId, String semester);

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

	/**
	 * Count schedules by room and semester (for utilization calculation).
	 */
	long countByRoomIdAndSemester(Long roomId, String semester);

	/**
	 * Count schedules by time slot and semester (for peak hours calculation).
	 */
	long countByTimeSlotIdAndSemester(Long timeSlotId, String semester);

	/**
	 * Count schedules for all rooms in a specific building for a semester.
	 */
	@Query("SELECT COUNT(s) FROM Schedule s WHERE s.room.building.id = :buildingId AND s.semester = :semester")
	long countSchedulesByBuildingAndSemester(@Param("buildingId") Long buildingId, @Param("semester") String semester);

	/**
	 * Delete all schedules for a specific semester.
	 */
	void deleteBySemester(String semester);
}
