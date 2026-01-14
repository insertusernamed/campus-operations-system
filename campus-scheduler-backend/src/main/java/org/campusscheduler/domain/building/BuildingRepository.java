package org.campusscheduler.domain.building;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Building entity database operations.
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    /**
     * Find a building by its unique code.
     *
     * @param code the building code
     * @return optional containing the building if found
     */
    Optional<Building> findByCode(String code);

    /**
     * Check if a building with the given code exists.
     *
     * @param code the building code
     * @return true if exists
     */
    boolean existsByCode(String code);
}
