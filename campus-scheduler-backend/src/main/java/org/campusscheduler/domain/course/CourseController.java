package org.campusscheduler.domain.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Course endpoints.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * Get all courses, optionally filtered by department or instructor.
     *
     * @param department   optional department filter
     * @param instructorId optional instructor filter
     * @return list of courses
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAll(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long instructorId) {
        if (instructorId != null) {
            return ResponseEntity.ok(courseService.findByInstructorId(instructorId));
        }
        if (department != null && !department.isBlank()) {
            return ResponseEntity.ok(courseService.findByDepartment(department));
        }
        return ResponseEntity.ok(courseService.findAll());
    }

    /**
     * Get a course by ID.
     *
     * @param id the course ID
     * @return the course if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getById(@PathVariable Long id) {
        return courseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a course by code.
     *
     * @param code the course code
     * @return the course if found
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Course> getByCode(@PathVariable String code) {
        return courseService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get courses by instructor.
     *
     * @param instructorId the instructor ID
     * @return list of courses taught by the instructor
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<List<Course>> getByInstructor(@PathVariable Long instructorId) {
        return ResponseEntity.ok(courseService.findByInstructorId(instructorId));
    }

    /**
     * Create a new course.
     *
     * @param course the course to create
     * @return the created course
     */
    @PostMapping
    public ResponseEntity<Course> create(@Valid @RequestBody Course course) {
        Course created = courseService.create(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create a new course with an instructor.
     *
     * @param instructorId the instructor ID
     * @param course       the course to create
     * @return the created course if instructor found
     */
    @PostMapping("/instructor/{instructorId}")
    public ResponseEntity<Course> createWithInstructor(
            @PathVariable Long instructorId,
            @Valid @RequestBody Course course) {
        return courseService.createWithInstructor(course, instructorId)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing course.
     *
     * @param id     the course ID
     * @param course the updated course data
     * @return the updated course if found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Course> update(@PathVariable Long id, @Valid @RequestBody Course course) {
        return courseService.update(id, course)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Assign an instructor to a course.
     *
     * @param courseId     the course ID
     * @param instructorId the instructor ID
     * @return the updated course if found
     */
    @PutMapping("/{courseId}/instructor/{instructorId}")
    public ResponseEntity<Course> assignInstructor(@PathVariable Long courseId, @PathVariable Long instructorId) {
        return courseService.assignInstructor(courseId, instructorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a course by ID.
     *
     * @param id the course ID
     * @return 204 if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (courseService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
