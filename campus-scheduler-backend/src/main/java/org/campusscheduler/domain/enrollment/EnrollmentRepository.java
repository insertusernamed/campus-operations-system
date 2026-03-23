package org.campusscheduler.domain.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository for Enrollment entity database operations.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Find all enrollments in a semester.
     *
     * @param semester the semester name
     * @return list of enrollments
     */
    List<Enrollment> findBySemester(String semester);

    /**
     * Find enrollments by course across semesters.
     *
     * @param courseId the course ID
     * @return list of enrollments
     */
    List<Enrollment> findByCourseId(Long courseId);

    /**
     * Find enrollments by schedule across semesters.
     *
     * @param scheduleId the schedule ID
     * @return list of enrollments
     */
    List<Enrollment> findByScheduleId(Long scheduleId);

    /**
     * Find enrollments for multiple schedules.
     *
     * @param scheduleIds the schedule IDs
     * @return list of enrollments
     */
    List<Enrollment> findByScheduleIdIn(Collection<Long> scheduleIds);

    /**
     * Find enrollments by student.
     *
     * @param studentId the student ID
     * @return list of enrollments
     */
    List<Enrollment> findByStudentId(Long studentId);

    /**
     * Find enrollments by student and semester.
     *
     * @param studentId the student ID
     * @param semester the semester name
     * @return list of enrollments
     */
    List<Enrollment> findByStudentIdAndSemester(Long studentId, String semester);

    /**
     * Find enrollments by course and semester.
     *
     * @param courseId the course ID
     * @param semester the semester name
     * @return list of enrollments
     */
    List<Enrollment> findByCourseIdAndSemester(Long courseId, String semester);

    /**
     * Find enrollments by schedule and semester.
     *
     * @param scheduleId the schedule ID
     * @param semester the semester name
     * @return list of enrollments
     */
    List<Enrollment> findByScheduleIdAndSemester(Long scheduleId, String semester);

    /**
     * Find enrollments by student, course, and semester.
     *
     * @param studentId the student ID
     * @param courseId the course ID
     * @param semester the semester name
     * @return list of enrollments
     */
    List<Enrollment> findByStudentIdAndCourseIdAndSemester(Long studentId, Long courseId, String semester);

    /**
     * Find enrollments by student, schedule, and semester.
     *
     * @param studentId the student ID
     * @param scheduleId the schedule ID
     * @param semester the semester name
     * @return list of enrollments
     */
    List<Enrollment> findByStudentIdAndScheduleIdAndSemester(Long studentId, Long scheduleId, String semester);

    /**
     * Delete all enrollments for a semester.
     *
     * @param semester the semester name
     * @return number of deleted rows
     */
    long deleteBySemester(String semester);
}
