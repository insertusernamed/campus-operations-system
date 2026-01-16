package org.campusscheduler.domain.instructor;

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
 * REST controller for Instructor endpoints.
 */
@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;

    /**
     * Get all instructors, optionally filtered by department.
     *
     * @param department optional department filter
     * @return list of instructors
     */
    @GetMapping
    public ResponseEntity<List<Instructor>> getAll(@RequestParam(required = false) String department) {
        if (department != null && !department.isBlank()) {
            return ResponseEntity.ok(instructorService.findByDepartment(department));
        }
        return ResponseEntity.ok(instructorService.findAll());
    }

    /**
     * Get an instructor by ID.
     *
     * @param id the instructor ID
     * @return the instructor if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Instructor> getById(@PathVariable Long id) {
        return instructorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new instructor.
     *
     * @param instructor the instructor to create
     * @return the created instructor
     */
    @PostMapping
    public ResponseEntity<Instructor> create(@Valid @RequestBody Instructor instructor) {
        Instructor created = instructorService.create(instructor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing instructor.
     *
     * @param id         the instructor ID
     * @param instructor the updated instructor data
     * @return the updated instructor if found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Instructor> update(@PathVariable Long id, @Valid @RequestBody Instructor instructor) {
        return instructorService.update(id, instructor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an instructor by ID.
     *
     * @param id the instructor ID
     * @return 204 if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (instructorService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
