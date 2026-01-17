package org.campusscheduler.domain.building;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Building endpoints.
 */
@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
@Tag(name = "Buildings", description = "Building management endpoints")
public class BuildingController {

    private final BuildingService buildingService;

    @Operation(summary = "Get all buildings", description = "Returns a list of all buildings in the system")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved buildings")
    @GetMapping
    public ResponseEntity<List<Building>> getAll() {
        return ResponseEntity.ok(buildingService.findAll());
    }

    @Operation(summary = "Get building by ID", description = "Returns a single building by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Building found"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Building> getById(
            @Parameter(description = "Building ID") @PathVariable Long id) {
        return buildingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new building", description = "Creates a new building with the provided data")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Building created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid building data")
    })
    @PostMapping
    public ResponseEntity<Building> create(@Valid @RequestBody Building building) {
        Building created = buildingService.create(building);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a building", description = "Updates an existing building by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Building updated successfully"),
            @ApiResponse(responseCode = "404", description = "Building not found"),
            @ApiResponse(responseCode = "400", description = "Invalid building data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Building> update(
            @Parameter(description = "Building ID") @PathVariable Long id,
            @Valid @RequestBody Building building) {
        return buildingService.update(id, building)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a building", description = "Deletes a building by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Building deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Building ID") @PathVariable Long id) {
        if (buildingService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
