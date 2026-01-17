package org.campusscheduler.domain.course;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Get all courses", description = "Returns all courses, optionally filtered by department or instructor")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved courses")
    @GetMapping
    public ResponseEntity<List<Course>> getAll(
            @Parameter(description = "Filter by department") @RequestParam(required = false) String department,
            @Parameter(description = "Filter by instructor ID") @RequestParam(required = false) Long instructorId) {
        if (instructorId != null) {
            return ResponseEntity.ok(courseService.findByInstructorId(instructorId));
        }
        if (department != null && !department.isBlank()) {
            return ResponseEntity.ok(courseService.findByDepartment(department));
        }
        return ResponseEntity.ok(courseService.findAll());
    }

    @Operation(summary = "Get course by ID", description = "Returns a single course by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course found"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Course> getById(
            @Parameter(description = "Course ID") @PathVariable Long id) {
        return courseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get course by code", description = "Returns a course by its unique code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course found"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<Course> getByCode(
            @Parameter(description = "Course code (e.g., CS101)") @PathVariable String code) {
        return courseService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get courses by instructor", description = "Returns all courses taught by a specific instructor")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved courses")
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<List<Course>> getByInstructor(
            @Parameter(description = "Instructor ID") @PathVariable Long instructorId) {
        return ResponseEntity.ok(courseService.findByInstructorId(instructorId));
    }

    @Operation(summary = "Create a new course", description = "Creates a new course without an instructor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Course created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid course data or code already exists")
    })
    @PostMapping
    public ResponseEntity<Course> create(@Valid @RequestBody Course course) {
        Course created = courseService.create(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Create course with instructor", description = "Creates a new course and assigns it to an instructor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Course created successfully"),
            @ApiResponse(responseCode = "404", description = "Instructor not found"),
            @ApiResponse(responseCode = "400", description = "Invalid course data")
    })
    @PostMapping("/instructor/{instructorId}")
    public ResponseEntity<Course> createWithInstructor(
            @Parameter(description = "Instructor ID") @PathVariable Long instructorId,
            @Valid @RequestBody Course course) {
        return courseService.createWithInstructor(course, instructorId)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a course", description = "Updates an existing course by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course updated successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "400", description = "Invalid data or code already in use")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Course> update(
            @Parameter(description = "Course ID") @PathVariable Long id,
            @Valid @RequestBody Course course) {
        return courseService.update(id, course)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Assign instructor to course", description = "Assigns an instructor to an existing course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructor assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Course or instructor not found")
    })
    @PutMapping("/{courseId}/instructor/{instructorId}")
    public ResponseEntity<Course> assignInstructor(
            @Parameter(description = "Course ID") @PathVariable Long courseId,
            @Parameter(description = "Instructor ID") @PathVariable Long instructorId) {
        return courseService.assignInstructor(courseId, instructorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a course", description = "Deletes a course by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Course ID") @PathVariable Long id) {
        if (courseService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
