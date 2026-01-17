package org.campusscheduler.domain.room;

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

import java.util.List;

/**
 * REST controller for Room endpoints.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management endpoints")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "Get all rooms", description = "Returns all rooms, optionally filtered by building")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms")
    @GetMapping
    public ResponseEntity<List<Room>> getAll(
            @Parameter(description = "Filter by building ID") @RequestParam(required = false) Long buildingId) {
        if (buildingId != null) {
            return ResponseEntity.ok(roomService.findByBuildingId(buildingId));
        }
        return ResponseEntity.ok(roomService.findAll());
    }

    @Operation(summary = "Get room by ID", description = "Returns a single room by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room found"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Room> getById(
            @Parameter(description = "Room ID") @PathVariable Long id) {
        return roomService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get rooms by type", description = "Returns rooms of a specific type")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms")
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Room>> getByType(
            @Parameter(description = "Room type (CLASSROOM, LECTURE_HALL, LAB, SEMINAR, CONFERENCE)") @PathVariable Room.RoomType type) {
        return ResponseEntity.ok(roomService.findByType(type));
    }

    @Operation(summary = "Get rooms by minimum capacity", description = "Returns rooms with at least the specified capacity")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved rooms")
    @GetMapping("/capacity/{capacity}")
    public ResponseEntity<List<Room>> getByMinCapacity(
            @Parameter(description = "Minimum capacity") @PathVariable Integer capacity) {
        return ResponseEntity.ok(roomService.findByMinCapacity(capacity));
    }

    @Operation(summary = "Create a new room", description = "Creates a new room in the specified building")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Room created successfully"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "400", description = "Invalid room data")
    })
    @PostMapping("/building/{buildingId}")
    public ResponseEntity<Room> create(
            @Parameter(description = "Building ID") @PathVariable Long buildingId,
            @Valid @RequestBody Room room) {
        return roomService.create(room, buildingId)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a room", description = "Updates an existing room by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "400", description = "Invalid room data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Room> update(
            @Parameter(description = "Room ID") @PathVariable Long id,
            @Valid @RequestBody Room room) {
        return roomService.update(id, room)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a room", description = "Deletes a room by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Room ID") @PathVariable Long id) {
        if (roomService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
