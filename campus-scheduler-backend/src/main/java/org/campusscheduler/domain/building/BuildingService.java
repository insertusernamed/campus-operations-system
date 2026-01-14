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
	 * @throws IllegalArgumentException if a building with the same code exists
	 */
	@Transactional
	public Building create(Building building) {
		String code = building.getCode();
		if (code != null && buildingRepository.existsByCode(code)) {
			throw new IllegalArgumentException("Building code already exists: " + code);
		}
		return buildingRepository.save(building);
	}

	/**
	 * Update an existing building.
	 *
	 * @param id      the building ID
	 * @param updated the updated building data
	 * @return optional containing the updated building if found
	 * @throws IllegalArgumentException if the new code is already in use
	 */
	@Transactional
	public Optional<Building> update(Long id, Building updated) {
		return buildingRepository.findById(id)
				.map(existing -> {
					String newCode = updated.getCode();
					if (newCode != null && !newCode.equals(existing.getCode())) {
						buildingRepository.findByCode(newCode)
								.filter(other -> !other.getId().equals(id))
								.ifPresent(other -> {
									throw new IllegalArgumentException("Building code already in use: " + newCode);
								});
					}
					existing.setName(updated.getName());
					existing.setCode(newCode);
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
		return buildingRepository.findById(id)
				.map(building -> {
					buildingRepository.deleteById(id);
					return true;
				})
				.orElse(false);
	}
}
