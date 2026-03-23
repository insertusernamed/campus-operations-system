package org.campusscheduler.domain.schedule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Schedule endpoints.
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Schedule management with conflict detection")
public class ScheduleController {

	private final ScheduleService scheduleService;
    private final ScheduleResponseService scheduleResponseService;

	@Operation(summary = "Get all schedules", description = "Returns all schedules, with optional filtering")
	@ApiResponse(responseCode = "200", description = "Successfully retrieved schedules")
	@GetMapping
	public ResponseEntity<List<ScheduleResponse>> getAll(
			@Parameter(description = "Filter by room ID") @RequestParam(required = false) Long roomId,
			@Parameter(description = "Filter by course ID") @RequestParam(required = false) Long courseId,
			@Parameter(description = "Filter by instructor ID") @RequestParam(required = false) Long instructorId,
			@Parameter(description = "Filter by semester") @RequestParam(required = false) String semester) {

		if (roomId != null) {
			return ResponseEntity.ok(scheduleResponseService.toResponses(scheduleService.findByRoomId(roomId)));
		}
		if (courseId != null) {
			return ResponseEntity.ok(scheduleResponseService.toResponses(scheduleService.findByCourseId(courseId)));
		}
		if (instructorId != null) {
			return ResponseEntity.ok(scheduleResponseService.toResponses(scheduleService.findByInstructorId(instructorId)));
		}
		if (semester != null && !semester.isBlank()) {
			return ResponseEntity.ok(scheduleResponseService.toResponses(scheduleService.findBySemester(semester)));
		}
		return ResponseEntity.ok(scheduleResponseService.toResponses(scheduleService.findAll()));
	}

	@Operation(summary = "Get schedule by ID", description = "Returns a single schedule by its ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Schedule found"),
			@ApiResponse(responseCode = "404", description = "Schedule not found")
	})
	@GetMapping("/{id}")
	public ResponseEntity<ScheduleResponse> getById(
			@Parameter(description = "Schedule ID") @PathVariable Long id) {
		return scheduleService.findById(id)
				.map(scheduleResponseService::toResponse)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Create a new schedule", description = "Creates a schedule linking course, room, and time slot")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "404", description = "Course, room, or time slot not found"),
			@ApiResponse(responseCode = "409", description = "Scheduling conflict (room booked or capacity exceeded)")
	})
	@PostMapping
	public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleCreateRequest request) {
		return scheduleService.create(
				request.getCourseId(),
				request.getRoomId(),
				request.getTimeSlotId(),
				request.getSemester())
				.map(scheduleResponseService::toResponse)
				.map(schedule -> ResponseEntity.status(HttpStatus.CREATED).body(schedule))
				.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Delete a schedule", description = "Deletes a schedule by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Schedule not found")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@Parameter(description = "Schedule ID") @PathVariable Long id) {
		if (scheduleService.delete(id)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}

	@Operation(summary = "Check for conflicts", description = "Checks if a room is available at a given time slot for a specific semester")
	@ApiResponse(responseCode = "200", description = "Conflict check completed")
	@GetMapping("/conflicts")
	public ResponseEntity<Map<String, Boolean>> checkConflicts(
			@Parameter(description = "Room ID") @RequestParam Long roomId,
			@Parameter(description = "Time slot ID") @RequestParam Long timeSlotId,
			@Parameter(description = "Semester (optional, if not provided checks any semester)") @RequestParam(required = false) String semester) {
		boolean hasConflict;
		if (semester != null && !semester.isBlank()) {
			hasConflict = scheduleService.hasRoomConflict(roomId, timeSlotId, semester);
		} else {
			hasConflict = scheduleService.hasRoomConflict(roomId, timeSlotId);
		}
		return ResponseEntity.ok(Map.of("hasConflict", hasConflict));
	}

	/**
	 * Handle scheduling conflict exceptions.
	 */
	@ExceptionHandler(ScheduleConflictException.class)
	public ResponseEntity<Map<String, String>> handleConflict(ScheduleConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("error", ex.getMessage()));
	}
}
