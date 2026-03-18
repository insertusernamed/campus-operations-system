package org.campusscheduler.domain.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Enrollment read operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * Find enrollments by student.
     *
     * @param studentId the student ID
     * @return list of enrollments
     */
    public List<Enrollment> findByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Find enrollments by student and semester.
     *
     * @param studentId the student ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByStudentAndSemester(Long studentId, String semester) {
        return enrollmentRepository.findByStudentIdAndSemester(studentId, semester);
    }

    /**
     * Find enrollments by course and semester.
     *
     * @param courseId the course ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByCourseAndSemester(Long courseId, String semester) {
        return enrollmentRepository.findByCourseIdAndSemester(courseId, semester);
    }

    /**
     * Find enrollments by schedule and semester.
     *
     * @param scheduleId the schedule ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByScheduleAndSemester(Long scheduleId, String semester) {
        return enrollmentRepository.findByScheduleIdAndSemester(scheduleId, semester);
    }

    /**
     * Find enrollments by student, course, and semester.
     *
     * @param studentId the student ID
     * @param courseId the course ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByStudentCourseAndSemester(Long studentId, Long courseId, String semester) {
        return enrollmentRepository.findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester);
    }

    /**
     * Find enrollments by student, schedule, and semester.
     *
     * @param studentId the student ID
     * @param scheduleId the schedule ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByStudentScheduleAndSemester(Long studentId, Long scheduleId, String semester) {
        return enrollmentRepository.findByStudentIdAndScheduleIdAndSemester(studentId, scheduleId, semester);
    }
}
