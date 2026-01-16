package org.campusscheduler.domain.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Course entity database operations.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Find a course by its code.
     *
     * @param code the course code
     * @return optional containing the course if found
     */
    Optional<Course> findByCode(String code);

    /**
     * Find courses by department.
     *
     * @param department the department
     * @return list of courses in the department
     */
    List<Course> findByDepartment(String department);

    /**
     * Find courses by instructor.
     *
     * @param instructorId the instructor ID
     * @return list of courses taught by the instructor
     */
    List<Course> findByInstructorId(Long instructorId);

    /**
     * Check if a course with the given code exists.
     *
     * @param code the course code
     * @return true if exists
     */
    boolean existsByCode(String code);
}
