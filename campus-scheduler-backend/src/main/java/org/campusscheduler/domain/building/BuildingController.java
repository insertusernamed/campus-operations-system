package org.campusscheduler.domain.building;

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
public class BuildingController {

    private final BuildingService buildingService;

    /**
     * Get all buildings.
     *
     * @return list of all buildings
     */
    @GetMapping
    public ResponseEntity<List<Building>> getAll() {
        return ResponseEntity.ok(buildingService.findAll());
    }

    /**
     * Get a building by ID.
     *
     * @param id the building ID
     * @return the building if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Building> getById(@PathVariable Long id) {
        return buildingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new building.
     *
     * @param building the building to create
     * @return the created building
     */
    @PostMapping
    public ResponseEntity<Building> create(@Valid @RequestBody Building building) {
        Building created = buildingService.create(building);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing building.
     *
     * @param id       the building ID
     * @param building the updated building data
     * @return the updated building if found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Building> update(@PathVariable Long id, @Valid @RequestBody Building building) {
        return buildingService.update(id, building)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a building by ID.
     *
     * @param id the building ID
     * @return 204 if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (buildingService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
