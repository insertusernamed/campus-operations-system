package org.campusscheduler.domain.timeslot;

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
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    /**
     * Get all time slots, optionally filtered by day of week.
     *
     * @param dayOfWeek optional day of week filter
     * @return list of time slots
     */
    @GetMapping
    public ResponseEntity<List<TimeSlot>> getAll(@RequestParam(required = false) DayOfWeek dayOfWeek) {
        if (dayOfWeek != null) {
            return ResponseEntity.ok(timeSlotService.findByDayOfWeek(dayOfWeek));
        }
        return ResponseEntity.ok(timeSlotService.findAll());
    }

    /**
     * Get a time slot by ID.
     *
     * @param id the time slot ID
     * @return the time slot if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimeSlot> getById(@PathVariable Long id) {
        return timeSlotService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new time slot.
     *
     * @param timeSlot the time slot to create
     * @return the created time slot
     */
    @PostMapping
    public ResponseEntity<TimeSlot> create(@Valid @RequestBody TimeSlot timeSlot) {
        TimeSlot created = timeSlotService.create(timeSlot);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing time slot.
     *
     * @param id       the time slot ID
     * @param timeSlot the updated time slot data
     * @return the updated time slot if found
     */
    @PutMapping("/{id}")
    public ResponseEntity<TimeSlot> update(@PathVariable Long id, @Valid @RequestBody TimeSlot timeSlot) {
        return timeSlotService.update(id, timeSlot)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a time slot by ID.
     *
     * @param id the time slot ID
     * @return 204 if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (timeSlotService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
