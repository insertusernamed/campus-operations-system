package org.campusscheduler.domain.timeslot;

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

import java.time.DayOfWeek;
import java.util.List;

/**
 * REST controller for TimeSlot endpoints.
 */
@RestController
@RequestMapping("/api/timeslots")
@RequiredArgsConstructor
@Tag(name = "Time Slots", description = "Time slot management for scheduling")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @Operation(summary = "Get all time slots", description = "Returns all time slots, optionally filtered by day of week")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved time slots")
    @GetMapping
    public ResponseEntity<List<TimeSlot>> getAll(
            @Parameter(description = "Filter by day of week (MONDAY, TUESDAY, etc.)") @RequestParam(required = false) DayOfWeek dayOfWeek) {
        if (dayOfWeek != null) {
            return ResponseEntity.ok(timeSlotService.findByDayOfWeek(dayOfWeek));
        }
        return ResponseEntity.ok(timeSlotService.findAll());
    }

    @Operation(summary = "Get time slot by ID", description = "Returns a single time slot by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time slot found"),
            @ApiResponse(responseCode = "404", description = "Time slot not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TimeSlot> getById(
            @Parameter(description = "Time slot ID") @PathVariable Long id) {
        return timeSlotService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new time slot", description = "Creates a new time slot with start/end times")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Time slot created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid time slot data (e.g., start time after end time)")
    })
    @PostMapping
    public ResponseEntity<TimeSlot> create(@Valid @RequestBody TimeSlot timeSlot) {
        TimeSlot created = timeSlotService.create(timeSlot);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a time slot", description = "Updates an existing time slot by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time slot updated successfully"),
            @ApiResponse(responseCode = "404", description = "Time slot not found"),
            @ApiResponse(responseCode = "400", description = "Invalid time slot data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TimeSlot> update(
            @Parameter(description = "Time slot ID") @PathVariable Long id,
            @Valid @RequestBody TimeSlot timeSlot) {
        return timeSlotService.update(id, timeSlot)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a time slot", description = "Deletes a time slot by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Time slot deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Time slot not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Time slot ID") @PathVariable Long id) {
        if (timeSlotService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
