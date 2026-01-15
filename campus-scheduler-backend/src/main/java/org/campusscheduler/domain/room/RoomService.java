package org.campusscheduler.domain.room;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.building.BuildingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Room business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;

    /**
     * Get all rooms.
     *
     * @return list of all rooms
     */
    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    /**
     * Find a room by ID.
     *
     * @param id the room ID
     * @return optional containing the room if found
     */
    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    /**
     * Find all rooms in a building.
     *
     * @param buildingId the building ID
     * @return list of rooms in the building
     */
    public List<Room> findByBuildingId(Long buildingId) {
        return roomRepository.findByBuildingId(buildingId);
    }

    /**
     * Find rooms by type.
     *
     * @param type the room type
     * @return list of rooms of that type
     */
    public List<Room> findByType(Room.RoomType type) {
        return roomRepository.findByType(type);
    }

    /**
     * Find rooms with minimum capacity.
     *
     * @param capacity minimum capacity
     * @return list of rooms meeting capacity requirement
     */
    public List<Room> findByMinCapacity(Integer capacity) {
        return roomRepository.findByCapacityGreaterThanEqual(capacity);
    }

    /**
     * Create a new room in a building.
     *
     * @param room       the room to create
     * @param buildingId the building ID
     * @return optional containing the created room, empty if building not found
     */
    @Transactional
    public Optional<Room> create(Room room, Long buildingId) {
        return buildingRepository.findById(buildingId)
                .map(building -> {
                    room.setBuilding(building);
                    return roomRepository.save(room);
                });
    }

    /**
     * Update an existing room.
     *
     * @param id      the room ID
     * @param updated the updated room data
     * @return optional containing the updated room if found
     */
    @Transactional
    public Optional<Room> update(Long id, Room updated) {
        return roomRepository.findById(id)
                .map(existing -> {
                    existing.setRoomNumber(updated.getRoomNumber());
                    existing.setCapacity(updated.getCapacity());
                    existing.setType(updated.getType());
                    existing.setFeatures(updated.getFeatures());
                    return roomRepository.save(existing);
                });
    }

    /**
     * Delete a room by ID.
     *
     * @param id the room ID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(Long id) {
        return roomRepository.findById(id)
                .map(room -> {
                    roomRepository.deleteById(id);
                    return true;
                })
                .orElse(false);
    }
}
