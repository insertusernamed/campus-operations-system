package org.campusscheduler.domain.roombooking;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for student room bookings and participant lookup.
 */
@RestController
@RequestMapping("/api/room-bookings")
@RequiredArgsConstructor
@Tag(name = "Room Bookings", description = "Student room booking visibility and management")
public class RoomBookingController {

    private final RoomBookingService roomBookingService;

    @Operation(summary = "Get visible room bookings", description = "Returns room bookings for calendar display")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved room bookings")
    @GetMapping
    public ResponseEntity<List<RoomBookingResponse>> getAll(
            @Parameter(description = "Semester filter") @RequestParam(required = false) String semester,
            @Parameter(description = "Filter to bookings involving this student") @RequestParam(required = false) Long studentId,
            @RequestHeader(value = "X-Viewer-Role", required = false) String viewerRole,
            @RequestHeader(value = "X-Viewer-Student-Id", required = false) Long viewerStudentId) {
        return ResponseEntity.ok(roomBookingService.findVisibleBookings(semester, studentId, viewerRole, viewerStudentId));
    }

    @Operation(summary = "Create a student room booking", description = "Creates a booking for an unused room slot")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "404", description = "Student, room, or time slot not found"),
            @ApiResponse(responseCode = "409", description = "Booking conflicts with room or student limits")
    })
    @PostMapping
    public ResponseEntity<RoomBookingResponse> create(
            @Valid @RequestBody CreateRoomBookingRequest request,
            @RequestHeader(value = "X-Viewer-Role", required = false) String viewerRole,
            @RequestHeader(value = "X-Viewer-Student-Id", required = false) Long viewerStudentId) {
        return roomBookingService.create(request, viewerRole, viewerStudentId)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Lookup students by email for booking participants",
            description = "Returns only basic student information plus whether they already have a class in that slot")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching students")
    @GetMapping("/student-search")
    public ResponseEntity<List<RoomBookingStudentLookupResponse>> searchStudents(
            @Parameter(description = "Email query") @RequestParam String query,
            @Parameter(description = "Semester") @RequestParam String semester,
            @Parameter(description = "Time slot ID") @RequestParam Long timeSlotId,
            @Parameter(description = "Student IDs to exclude from suggestions")
            @RequestParam(required = false, name = "excludeStudentId") List<Long> excludeStudentIds) {
        return ResponseEntity.ok(
                roomBookingService.searchStudentsByEmail(query, semester, timeSlotId, excludeStudentIds == null ? List.of() : excludeStudentIds));
    }

    @ExceptionHandler(RoomBookingConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(RoomBookingConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
