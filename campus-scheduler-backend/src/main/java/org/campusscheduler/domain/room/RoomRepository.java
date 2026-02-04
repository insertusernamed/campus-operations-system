package org.campusscheduler.domain.room;

import org.campusscheduler.domain.building.Building;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Room entity database operations.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

	@Override
	@EntityGraph(attributePaths = { "building" })
	List<Room> findAll();

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
	@Query("SELECT r FROM Room r WHERE r.building.id = :buildingId")
	List<Room> findByBuildingId(@Param("buildingId") Long buildingId);

	/**
	 * Find a room by room number within a building.
	 *
	 * @param roomNumber the room number
	 * @param buildingId the building ID
	 * @return optional containing the room if found
	 */
	@Query("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber AND r.building.id = :buildingId")
	Optional<Room> findByRoomNumberAndBuildingId(@Param("roomNumber") String roomNumber, @Param("buildingId") Long buildingId);

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
