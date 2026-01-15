package org.campusscheduler.domain.room;

import org.campusscheduler.domain.building.Building;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for Room entity.
 */
@DataJpaTest
class RoomRepositoryTest {

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private org.campusscheduler.domain.building.BuildingRepository buildingRepository;

	private Building scienceBuilding;
	private Building artsBuilding;

	@BeforeEach
	void setUp() {
		scienceBuilding = Building.builder()
				.name("Science Building")
				.code("SCI")
				.build();
		buildingRepository.save(scienceBuilding);

		artsBuilding = Building.builder()
				.name("Arts Building")
				.code("ART")
				.build();
		buildingRepository.save(artsBuilding);
	}

	@Test
	@DisplayName("should find rooms by building ID")
	void shouldFindRoomsByBuildingId() {
		Room room1 = Room.builder()
				.roomNumber("101")
				.capacity(30)
				.type(Room.RoomType.CLASSROOM)
				.building(scienceBuilding)
				.build();
		Room room2 = Room.builder()
				.roomNumber("102")
				.capacity(25)
				.type(Room.RoomType.LAB)
				.building(scienceBuilding)
				.build();
		roomRepository.save(room1);
		roomRepository.save(room2);

		List<Room> result = roomRepository.findByBuildingId(scienceBuilding.getId());

		assertThat(result).hasSize(2);
	}

	@Test
	@DisplayName("should find room by room number and building ID")
	void shouldFindRoomByRoomNumberAndBuildingId() {
		Room room = Room.builder()
				.roomNumber("101")
				.capacity(30)
				.type(Room.RoomType.CLASSROOM)
				.building(scienceBuilding)
				.build();
		roomRepository.save(room);

		Optional<Room> result = roomRepository.findByRoomNumberAndBuildingId("101", scienceBuilding.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getRoomNumber()).isEqualTo("101");
	}

	@Test
	@DisplayName("should return empty when room number not found in building")
	void shouldReturnEmptyWhenRoomNumberNotFoundInBuilding() {
		Optional<Room> result = roomRepository.findByRoomNumberAndBuildingId("999", scienceBuilding.getId());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("should find rooms by type")
	void shouldFindRoomsByType() {
		Room classroom = Room.builder()
				.roomNumber("101")
				.capacity(30)
				.type(Room.RoomType.CLASSROOM)
				.building(scienceBuilding)
				.build();
		Room lab = Room.builder()
				.roomNumber("102")
				.capacity(20)
				.type(Room.RoomType.LAB)
				.building(scienceBuilding)
				.build();
		roomRepository.save(classroom);
		roomRepository.save(lab);

		List<Room> classrooms = roomRepository.findByType(Room.RoomType.CLASSROOM);
		List<Room> labs = roomRepository.findByType(Room.RoomType.LAB);

		assertThat(classrooms).hasSize(1);
		assertThat(labs).hasSize(1);
	}

	@Test
	@DisplayName("should find rooms by minimum capacity")
	void shouldFindRoomsByMinimumCapacity() {
		Room smallRoom = Room.builder()
				.roomNumber("101")
				.capacity(20)
				.type(Room.RoomType.SEMINAR)
				.building(scienceBuilding)
				.build();
		Room largeRoom = Room.builder()
				.roomNumber("LH1")
				.capacity(100)
				.type(Room.RoomType.LECTURE_HALL)
				.building(scienceBuilding)
				.build();
		roomRepository.save(smallRoom);
		roomRepository.save(largeRoom);

		List<Room> largeRooms = roomRepository.findByCapacityGreaterThanEqual(50);

		assertThat(largeRooms).hasSize(1);
		assertThat(largeRooms.get(0).getCapacity()).isEqualTo(100);
	}

	@Test
	@DisplayName("should find rooms by building entity")
	void shouldFindRoomsByBuildingEntity() {
		Room room = Room.builder()
				.roomNumber("101")
				.capacity(30)
				.type(Room.RoomType.CLASSROOM)
				.building(scienceBuilding)
				.build();
		roomRepository.save(room);

		List<Room> result = roomRepository.findByBuilding(scienceBuilding);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("should save room with all fields")
	void shouldSaveRoomWithAllFields() {
		Room room = Room.builder()
				.roomNumber("CONF1")
				.capacity(15)
				.type(Room.RoomType.CONFERENCE)
				.features("Video conferencing, Whiteboard")
				.building(artsBuilding)
				.build();

		Room saved = roomRepository.save(room);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getFeatures()).contains("Video conferencing");
	}
}
