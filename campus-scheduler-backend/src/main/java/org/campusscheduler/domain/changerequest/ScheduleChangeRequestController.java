package org.campusscheduler.domain.changerequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for schedule change requests.
 */
@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
@Tag(name = "Change Requests", description = "Schedule change request workflow")
public class ScheduleChangeRequestController {

    private final ScheduleChangeRequestService changeRequestService;

    @Operation(summary = "Get all change requests", description = "Returns all change requests with optional filtering")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved change requests")
    @GetMapping
    public ResponseEntity<List<ScheduleChangeRequest>> getAll(
            @Parameter(description = "Filter by status") @RequestParam(required = false) ChangeRequestStatus status,
            @Parameter(description = "Filter by instructor ID") @RequestParam(required = false) Long instructorId,
            @Parameter(description = "Filter by semester") @RequestParam(required = false) String semester,
            @Parameter(description = "Filter by schedule ID") @RequestParam(required = false) Long scheduleId) {
        return ResponseEntity.ok(changeRequestService.findAll(status, instructorId, semester, scheduleId));
    }

    @Operation(summary = "Get change request by ID", description = "Returns a single change request by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Change request found"),
            @ApiResponse(responseCode = "404", description = "Change request not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleChangeRequest> getById(
            @Parameter(description = "Change request ID") @PathVariable Long id) {
        return changeRequestService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a change request", description = "Creates a new schedule change request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Change request created successfully"),
            @ApiResponse(responseCode = "404", description = "Schedule or instructor not found")
    })
    @PostMapping
    public ResponseEntity<ScheduleChangeRequest> create(@Valid @RequestBody ChangeRequestCreateRequest request) {
        return changeRequestService.create(request)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Approve a change request", description = "Approves a change request and applies the schedule update")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Change request approved"),
            @ApiResponse(responseCode = "404", description = "Change request or proposed data not found"),
            @ApiResponse(responseCode = "409", description = "Change request conflicts with existing schedules")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<ScheduleChangeRequest> approve(
            @Parameter(description = "Change request ID") @PathVariable Long id,
            @Valid @RequestBody ChangeRequestDecisionRequest decisionRequest) {
        return changeRequestService.approve(id, decisionRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Reject a change request", description = "Rejects a change request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Change request rejected"),
            @ApiResponse(responseCode = "404", description = "Change request not found"),
            @ApiResponse(responseCode = "409", description = "Change request already resolved")
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<ScheduleChangeRequest> reject(
            @Parameter(description = "Change request ID") @PathVariable Long id,
            @Valid @RequestBody ChangeRequestDecisionRequest decisionRequest) {
        return changeRequestService.reject(id, decisionRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Validate a change request", description = "Checks for conflicts and warnings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation completed"),
            @ApiResponse(responseCode = "404", description = "Schedule or proposed data not found")
    })
    @PostMapping("/validate")
    public ResponseEntity<ChangeRequestValidationResponse> validate(
            @Valid @RequestBody ChangeRequestValidationRequest request) {
        return changeRequestService.validate(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(ChangeRequestConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ChangeRequestConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "hardConflicts", ex.getHardConflicts()
                ));
    }

    @ExceptionHandler(ChangeRequestStateException.class)
    public ResponseEntity<Map<String, Object>> handleStateError(ChangeRequestStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "code", "CHANGE_REQUEST_STATE"
                ));
    }
}
