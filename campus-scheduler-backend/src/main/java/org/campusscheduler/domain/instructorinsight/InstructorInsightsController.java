package org.campusscheduler.domain.instructorinsight;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
