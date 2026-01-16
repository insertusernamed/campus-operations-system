package org.campusscheduler.domain.instructor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Instructor entity database operations.
 */
@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    /**
     * Find an instructor by email.
     *
     * @param email the email address
     * @return optional containing the instructor if found
     */
    Optional<Instructor> findByEmail(String email);

    /**
     * Find instructors by department.
     *
     * @param department the department name
     * @return list of instructors in the department
     */
    List<Instructor> findByDepartment(String department);

    /**
     * Check if an instructor with the given email exists.
     *
     * @param email the email address
     * @return true if exists
     */
    boolean existsByEmail(String email);
}
