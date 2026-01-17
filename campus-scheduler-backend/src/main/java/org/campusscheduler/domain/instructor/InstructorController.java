package org.campusscheduler.domain.instructor;

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
 * REST controller for Instructor endpoints.
 */
@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
@Tag(name = "Instructors", description = "Instructor management endpoints")
public class InstructorController {

    private final InstructorService instructorService;

    @Operation(summary = "Get all instructors", description = "Returns all instructors, optionally filtered by department")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved instructors")
    @GetMapping
    public ResponseEntity<List<Instructor>> getAll(
            @Parameter(description = "Filter by department") @RequestParam(required = false) String department) {
        if (department != null && !department.isBlank()) {
            return ResponseEntity.ok(instructorService.findByDepartment(department));
        }
        return ResponseEntity.ok(instructorService.findAll());
    }

    @Operation(summary = "Get instructor by ID", description = "Returns a single instructor by their ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructor found"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Instructor> getById(
            @Parameter(description = "Instructor ID") @PathVariable Long id) {
        return instructorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new instructor", description = "Creates a new instructor with the provided data")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Instructor created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid instructor data or email already exists")
    })
    @PostMapping
    public ResponseEntity<Instructor> create(@Valid @RequestBody Instructor instructor) {
        Instructor created = instructorService.create(instructor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update an instructor", description = "Updates an existing instructor by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructor updated successfully"),
            @ApiResponse(responseCode = "404", description = "Instructor not found"),
            @ApiResponse(responseCode = "400", description = "Invalid data or email already in use")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Instructor> update(
            @Parameter(description = "Instructor ID") @PathVariable Long id,
            @Valid @RequestBody Instructor instructor) {
        return instructorService.update(id, instructor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete an instructor", description = "Deletes an instructor by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Instructor deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Instructor ID") @PathVariable Long id) {
        if (instructorService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
