package org.campusscheduler.domain.instructorinsight;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for proactive instructor schedule friction insights.
 */
@RestController
@RequestMapping("/api/instructor-insights")
@RequiredArgsConstructor
@Validated
@Tag(name = "Instructor Insights", description = "Instructor-focused schedule frictions")
public class InstructorInsightsController {

    private final InstructorInsightsService instructorInsightsService;

    @GetMapping("/summary")
    @Operation(summary = "Get instructor operations snapshot")
    @ApiResponse(responseCode = "200", description = "Snapshot returned")
    public ResponseEntity<InstructorInsightsSummaryResponse> getSummary(
            @Parameter(description = "Semester") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return ResponseEntity.ok(instructorInsightsService.getSummary(semester));
    }

    @GetMapping("/queue")
    @Operation(summary = "Get prioritized instructor queue")
    @ApiResponse(responseCode = "200", description = "Queue rows returned")
    public ResponseEntity<List<InstructorQueueRowResponse>> getQueue(
            @Parameter(description = "Semester") @RequestParam @jakarta.validation.constraints.NotBlank String semester,
            @Parameter(description = "Queue filter") @RequestParam(required = false) String filter,
            @Parameter(description = "Department filter") @RequestParam(required = false) String department) {
        return ResponseEntity.ok(instructorInsightsService.getQueue(semester, filter, department));
    }

    @GetMapping("/load-distribution")
    @Operation(summary = "Get department load and coverage distribution")
    @ApiResponse(responseCode = "200", description = "Load distribution returned")
    public ResponseEntity<InstructorLoadDistributionResponse> getLoadDistribution(
            @Parameter(description = "Semester") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return ResponseEntity.ok(instructorInsightsService.getLoadDistribution(semester));
    }

    @GetMapping("/{id}/workbench")
    @Operation(summary = "Get instructor operational workbench")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workbench returned"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    public ResponseEntity<InstructorWorkbenchResponse> getWorkbench(
            @Parameter(description = "Instructor ID") @PathVariable Long id,
            @Parameter(description = "Semester") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return instructorInsightsService.getWorkbench(id, semester)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/frictions")
    @Operation(summary = "Get instructor schedule frictions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friction insights returned"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    public ResponseEntity<List<InstructorFrictionIssueResponse>> getFrictions(
            @Parameter(description = "Instructor ID") @RequestParam @jakarta.validation.constraints.NotNull Long instructorId,
            @Parameter(description = "Semester") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return instructorInsightsService.findFrictions(instructorId, semester)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
