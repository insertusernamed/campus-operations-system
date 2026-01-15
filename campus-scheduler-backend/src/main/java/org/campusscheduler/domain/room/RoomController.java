package org.campusscheduler.domain.room;

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
public class RoomController {

    private final RoomService roomService;

    /**
     * Get all rooms, optionally filtered by building.
     *
     * @param buildingId optional building ID filter
     * @return list of rooms
     */
    @GetMapping
    public ResponseEntity<List<Room>> getAll(@RequestParam(required = false) Long buildingId) {
        if (buildingId != null) {
            return ResponseEntity.ok(roomService.findByBuildingId(buildingId));
        }
        return ResponseEntity.ok(roomService.findAll());
    }

    /**
     * Get a room by ID.
     *
     * @param id the room ID
     * @return the room if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Room> getById(@PathVariable Long id) {
        return roomService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get rooms by type.
     *
     * @param type the room type
     * @return list of rooms of that type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Room>> getByType(@PathVariable Room.RoomType type) {
        return ResponseEntity.ok(roomService.findByType(type));
    }

    /**
     * Get rooms with minimum capacity.
     *
     * @param capacity minimum capacity
     * @return list of rooms meeting capacity requirement
     */
    @GetMapping("/capacity/{capacity}")
    public ResponseEntity<List<Room>> getByMinCapacity(@PathVariable Integer capacity) {
        return ResponseEntity.ok(roomService.findByMinCapacity(capacity));
    }

    /**
     * Create a new room in a building.
     *
     * @param buildingId the building ID
     * @param room       the room to create
     * @return the created room
     */
    @PostMapping("/building/{buildingId}")
    public ResponseEntity<Room> create(@PathVariable Long buildingId, @Valid @RequestBody Room room) {
        return roomService.create(room, buildingId)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing room.
     *
     * @param id   the room ID
     * @param room the updated room data
     * @return the updated room if found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Room> update(@PathVariable Long id, @Valid @RequestBody Room room) {
        return roomService.update(id, room)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a room by ID.
     *
     * @param id the room ID
     * @return 204 if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (roomService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
