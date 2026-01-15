package org.campusscheduler.domain.room;

import org.campusscheduler.domain.building.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Room entity database operations.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Find all rooms in a building.
     *
     * @param building the building
     * @return list of rooms in the building
     */
    List<Room> findByBuilding(Building building);

    /**
     * Find all rooms in a building by building ID.
     *
     * @param buildingId the building ID
     * @return list of rooms
     */
    List<Room> findByBuildingId(Long buildingId);

    /**
     * Find a room by room number within a building.
     *
     * @param roomNumber the room number
     * @param buildingId the building ID
     * @return optional containing the room if found
     */
    Optional<Room> findByRoomNumberAndBuildingId(String roomNumber, Long buildingId);

    /**
     * Find rooms by type.
     *
     * @param type the room type
     * @return list of rooms of that type
     */
    List<Room> findByType(Room.RoomType type);

    /**
     * Find rooms with capacity greater than or equal to specified value.
     *
     * @param capacity minimum capacity
     * @return list of rooms meeting capacity requirement
     */
    List<Room> findByCapacityGreaterThanEqual(Integer capacity);
}
