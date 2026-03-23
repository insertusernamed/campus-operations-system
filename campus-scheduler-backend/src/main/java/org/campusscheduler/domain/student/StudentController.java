package org.campusscheduler.domain.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentResponse;
import org.campusscheduler.domain.enrollment.EnrollmentService;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * REST controller for read-only student endpoints.
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student roster and schedule lookup")
public class StudentController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

    @Operation(summary = "Get all students", description = "Returns all generated students")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved students")
    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAll() {
        return ResponseEntity.ok(studentService.findAll().stream()
                .map(StudentResponse::from)
                .toList());
    }

    @Operation(summary = "Get student by ID", description = "Returns a single student by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student found"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getById(
            @Parameter(description = "Student ID") @PathVariable Long id) {
        return studentService.findById(id)
                .map(StudentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get student schedule by semester",
            description = "Returns the student's enrolled and waitlisted classes for a semester")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student schedule found"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/{id}/schedule")
    public ResponseEntity<StudentScheduleResponse> getSchedule(
            @Parameter(description = "Student ID") @PathVariable Long id,
            @Parameter(description = "Semester name") @RequestParam String semester) {
        if (studentService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<EnrollmentResponse> responses = enrollmentService.findByStudentAndSemester(id, semester).stream()
                .sorted(enrollmentComparator())
                .map(EnrollmentResponse::from)
                .toList();

        List<EnrollmentResponse> enrolled = responses.stream()
                .filter(response -> response.status() == EnrollmentStatus.ENROLLED)
                .toList();
        List<EnrollmentResponse> waitlisted = responses.stream()
                .filter(response -> response.status() == EnrollmentStatus.WAITLISTED)
                .toList();

        return ResponseEntity.ok(new StudentScheduleResponse(id, semester, enrolled, waitlisted));
    }

    private Comparator<Enrollment> enrollmentComparator() {
        return Comparator
                .comparing((Enrollment enrollment) -> enrollment.getSchedule() != null
                        && enrollment.getSchedule().getTimeSlot() != null
                        && enrollment.getSchedule().getTimeSlot().getDayOfWeek() != null
                                ? enrollment.getSchedule().getTimeSlot().getDayOfWeek().getValue()
                                : Integer.MAX_VALUE)
                .thenComparing(enrollment -> enrollment.getSchedule() != null
                        && enrollment.getSchedule().getTimeSlot() != null
                        && enrollment.getSchedule().getTimeSlot().getStartTime() != null
                                ? enrollment.getSchedule().getTimeSlot().getStartTime()
                                : java.time.LocalTime.MAX)
                .thenComparing(enrollment -> enrollment.getSchedule() != null
                        && enrollment.getSchedule().getCourse() != null
                        && enrollment.getSchedule().getCourse().getCode() != null
                                ? enrollment.getSchedule().getCourse().getCode()
                                : "")
                .thenComparing(enrollment -> enrollment.getId() == null ? Long.MAX_VALUE : enrollment.getId());
    }
}
