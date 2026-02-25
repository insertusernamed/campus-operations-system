package org.campusscheduler.domain.instructorpreference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for instructor preference profiles.
 */
@RestController
@RequestMapping("/api/instructor-preferences")
@RequiredArgsConstructor
@Tag(name = "Instructor Preferences", description = "Instructor scheduling preference profiles")
public class InstructorPreferenceController {

    private final InstructorPreferenceService instructorPreferenceService;

    @GetMapping("/{instructorId}")
    @Operation(summary = "Get instructor preferences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences returned"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    public ResponseEntity<InstructorPreferenceResponse> getByInstructorId(
            @Parameter(description = "Instructor ID") @PathVariable Long instructorId) {
        return instructorPreferenceService.getByInstructorId(instructorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{instructorId}")
    @Operation(summary = "Create or update instructor preferences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences saved"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    public ResponseEntity<InstructorPreferenceResponse> upsert(
            @Parameter(description = "Instructor ID") @PathVariable Long instructorId,
            @Valid @RequestBody InstructorPreferenceUpdateRequest request) {
        return instructorPreferenceService.upsert(instructorId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
