package org.campusscheduler.solver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for solver impact analysis.
 */
@RestController
@RequestMapping("/api/solver/impact")
@RequiredArgsConstructor
@Tag(name = "Solver Impact", description = "Analyze schedule change impacts")
public class ImpactAnalysisController {

    private final ImpactAnalysisService impactAnalysisService;

    @PostMapping
    @Operation(summary = "Analyze impact", description = "Runs a sandbox solver to suggest a minimal swap")
    @ApiResponse(responseCode = "200", description = "Impact analysis completed")
    public ResponseEntity<ImpactAnalysisResponse> analyze(@Valid @RequestBody ImpactAnalysisRequest request) {
        return impactAnalysisService.analyze(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler(ImpactAnalysisStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(ImpactAnalysisStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "code", "IMPACT_STATE"
                ));
    }
}
