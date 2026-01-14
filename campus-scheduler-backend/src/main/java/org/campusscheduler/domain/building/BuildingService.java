package org.campusscheduler.domain.building;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Building business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;

    /**
     * Get all buildings.
     *
     * @return list of all buildings
     */
    public List<Building> findAll() {
        return buildingRepository.findAll();
    }

    /**
     * Find a building by ID.
     *
     * @param id the building ID
     * @return optional containing the building if found
     */
    public Optional<Building> findById(Long id) {
        return buildingRepository.findById(id);
    }

    /**
     * Find a building by its code.
     *
     * @param code the building code
     * @return optional containing the building if found
     */
    public Optional<Building> findByCode(String code) {
        return buildingRepository.findByCode(code);
    }

    /**
     * Create a new building.
     *
     * @param building the building to create
     * @return the created building
     */
    @Transactional
    public Building create(Building building) {
        return buildingRepository.save(building);
    }

    /**
     * Update an existing building.
     *
     * @param id      the building ID
     * @param updated the updated building data
     * @return optional containing the updated building if found
     */
    @Transactional
    public Optional<Building> update(Long id, Building updated) {
        return buildingRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setCode(updated.getCode());
                    existing.setAddress(updated.getAddress());
                    return buildingRepository.save(existing);
                });
    }

    /**
     * Delete a building by ID.
     *
     * @param id the building ID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(Long id) {
        if (buildingRepository.existsById(id)) {
            buildingRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
