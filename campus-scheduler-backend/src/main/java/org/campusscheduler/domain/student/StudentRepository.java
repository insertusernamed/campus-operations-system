package org.campusscheduler.domain.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Student entity database operations.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Find a student by student number.
     *
     * @param studentNumber the student number
     * @return optional containing the student if found
     */
    Optional<Student> findByStudentNumber(String studentNumber);

    /**
     * Find a student by email.
     *
     * @param email the email address
     * @return optional containing the student if found
     */
    Optional<Student> findByEmail(String email);

    /**
     * Find all students in a department.
     *
     * @param department the department name
     * @return list of students in the department
     */
    List<Student> findByDepartment(String department);

    /**
     * Find all students in a department and year level.
     *
     * @param department the department name
     * @param yearLevel the year level
     * @return list of matching students
     */
    List<Student> findByDepartmentAndYearLevel(String department, Integer yearLevel);

    /**
     * Check if a student number already exists.
     *
     * @param studentNumber the student number
     * @return true if it exists
     */
    boolean existsByStudentNumber(String studentNumber);

    /**
     * Check if an email already exists.
     *
     * @param email the email address
     * @return true if it exists
     */
    boolean existsByEmail(String email);

    /**
     * Count all generated course requests across students.
     *
     * @return number of ranked course preference entries
     */
    @Query("select count(preferenceCourseId) from Student s join s.preferredCourseIds preferenceCourseId")
    long countPreferredCourseRequests();
}
