package org.campusscheduler.domain.enrollment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for read-only enrollment and waitlist queries.
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment and waitlist lookup")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(
            summary = "Get enrollments",
            description = "Returns enrollments filtered by student, course, schedule, and semester")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved enrollments")
    @GetMapping
    public ResponseEntity<List<EnrollmentResponse>> getAll(
            @Parameter(description = "Filter by student ID") @RequestParam(required = false) Long studentId,
            @Parameter(description = "Filter by course ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "Filter by schedule ID") @RequestParam(required = false) Long scheduleId,
            @Parameter(description = "Filter by semester") @RequestParam(required = false) String semester) {
        return ResponseEntity.ok(enrollmentService.findByFilters(studentId, courseId, scheduleId, semester).stream()
                .map(EnrollmentResponse::from)
                .toList());
    }
}
