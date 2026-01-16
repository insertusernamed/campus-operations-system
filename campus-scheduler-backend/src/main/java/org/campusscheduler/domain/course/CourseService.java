package org.campusscheduler.domain.course;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Course business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;

    /**
     * Get all courses.
     *
     * @return list of all courses
     */
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    /**
     * Find a course by ID.
     *
     * @param id the course ID
     * @return optional containing the course if found
     */
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    /**
     * Find a course by code.
     *
     * @param code the course code
     * @return optional containing the course if found
     */
    public Optional<Course> findByCode(String code) {
        return courseRepository.findByCode(code);
    }

    /**
     * Find courses by department.
     *
     * @param department the department
     * @return list of courses in the department
     */
    public List<Course> findByDepartment(String department) {
        return courseRepository.findByDepartment(department);
    }

    /**
     * Find courses by instructor.
     *
     * @param instructorId the instructor ID
     * @return list of courses taught by the instructor
     */
    public List<Course> findByInstructorId(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    /**
     * Create a new course.
     *
     * @param course the course to create
     * @return the created course
     */
    @Transactional
    public Course create(Course course) {
        return courseRepository.save(course);
    }

    /**
     * Create a new course with an instructor.
     *
     * @param course       the course to create
     * @param instructorId the instructor ID
     * @return optional containing the created course, empty if instructor not found
     */
    @Transactional
    public Optional<Course> createWithInstructor(Course course, Long instructorId) {
        return instructorRepository.findById(instructorId)
                .map(instructor -> {
                    course.setInstructor(instructor);
                    return courseRepository.save(course);
                });
    }

    /**
     * Update an existing course.
     *
     * @param id      the course ID
     * @param updated the updated course data
     * @return optional containing the updated course if found
     */
    @Transactional
    public Optional<Course> update(Long id, Course updated) {
        return courseRepository.findById(id)
                .map(existing -> {
                    existing.setCode(updated.getCode());
                    existing.setName(updated.getName());
                    existing.setDescription(updated.getDescription());
                    existing.setCredits(updated.getCredits());
                    existing.setEnrollmentCapacity(updated.getEnrollmentCapacity());
                    existing.setDepartment(updated.getDepartment());
                    return courseRepository.save(existing);
                });
    }

    /**
     * Assign an instructor to a course.
     *
     * @param courseId     the course ID
     * @param instructorId the instructor ID
     * @return optional containing the updated course, empty if not found
     */
    @Transactional
    public Optional<Course> assignInstructor(Long courseId, Long instructorId) {
        return courseRepository.findById(courseId)
                .flatMap(course -> instructorRepository.findById(instructorId)
                        .map(instructor -> {
                            course.setInstructor(instructor);
                            return courseRepository.save(course);
                        }));
    }

    /**
     * Delete a course by ID.
     *
     * @param id the course ID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(Long id) {
        if (courseRepository.existsById(id)) {
            courseRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
