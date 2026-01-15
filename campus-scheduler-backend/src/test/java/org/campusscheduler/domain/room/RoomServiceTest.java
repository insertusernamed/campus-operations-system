package org.campusscheduler.domain.room;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RoomService.
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private BuildingRepository buildingRepository;

	@InjectMocks
	private RoomService roomService;

	private Building testBuilding;
	private Room testRoom;

	@BeforeEach
	void setUp() {
		testBuilding = Building.builder()
				.id(1L)
				.name("Science Building")
				.code("SCI")
				.build();

		testRoom = Room.builder()
				.id(1L)
				.roomNumber("101")
				.capacity(30)
				.type(Room.RoomType.CLASSROOM)
				.features("Projector, Whiteboard")
				.building(testBuilding)
				.build();
	}

	@Nested
	@DisplayName("findAll")
	class FindAll {

		@Test
		@DisplayName("should return all rooms")
		void shouldReturnAllRooms() {
			Room room2 = Room.builder()
					.id(2L)
					.roomNumber("102")
					.capacity(50)
					.type(Room.RoomType.LECTURE_HALL)
					.build();

			when(roomRepository.findAll()).thenReturn(List.of(testRoom, room2));

			List<Room> result = roomService.findAll();

			assertThat(result).hasSize(2);
			verify(roomRepository).findAll();
		}

		@Test
		@DisplayName("should return empty list when no rooms exist")
		void shouldReturnEmptyListWhenNoRoomsExist() {
			when(roomRepository.findAll()).thenReturn(List.of());

			List<Room> result = roomService.findAll();

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("findById")
	class FindById {

		@Test
		@DisplayName("should return room when found")
		void shouldReturnRoomWhenFound() {
			when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

			Optional<Room> result = roomService.findById(1L);

			assertThat(result).isPresent();
			assertThat(result.get().getRoomNumber()).isEqualTo("101");
		}

		@Test
		@DisplayName("should return empty when not found")
		void shouldReturnEmptyWhenNotFound() {
			when(roomRepository.findById(999L)).thenReturn(Optional.empty());

			Optional<Room> result = roomService.findById(999L);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("findByBuildingId")
	class FindByBuildingId {

		@Test
		@DisplayName("should return rooms in building")
		void shouldReturnRoomsInBuilding() {
			when(roomRepository.findByBuildingId(1L)).thenReturn(List.of(testRoom));

			List<Room> result = roomService.findByBuildingId(1L);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getRoomNumber()).isEqualTo("101");
		}

		@Test
		@DisplayName("should return empty list when building has no rooms")
		void shouldReturnEmptyListWhenBuildingHasNoRooms() {
			when(roomRepository.findByBuildingId(999L)).thenReturn(List.of());

			List<Room> result = roomService.findByBuildingId(999L);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	@DisplayName("findByType")
	class FindByType {

		@Test
		@DisplayName("should return rooms of specified type")
		void shouldReturnRoomsOfSpecifiedType() {
			when(roomRepository.findByType(Room.RoomType.CLASSROOM)).thenReturn(List.of(testRoom));

			List<Room> result = roomService.findByType(Room.RoomType.CLASSROOM);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getType()).isEqualTo(Room.RoomType.CLASSROOM);
		}
	}

	@Nested
	@DisplayName("findByMinCapacity")
	class FindByMinCapacity {

		@Test
		@DisplayName("should return rooms with capacity >= specified value")
		void shouldReturnRoomsWithSufficientCapacity() {
			Room largeRoom = Room.builder()
					.id(2L)
					.roomNumber("LH1")
					.capacity(100)
					.type(Room.RoomType.LECTURE_HALL)
					.build();

			when(roomRepository.findByCapacityGreaterThanEqual(50)).thenReturn(List.of(largeRoom));

			List<Room> result = roomService.findByMinCapacity(50);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getCapacity()).isGreaterThanOrEqualTo(50);
		}
	}

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		@DisplayName("should create room when building exists")
		void shouldCreateRoomWhenBuildingExists() {
			Room newRoom = Room.builder()
					.roomNumber("201")
					.capacity(25)
					.type(Room.RoomType.SEMINAR)
					.build();

			when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
			when(roomRepository.save(any(Room.class))).thenAnswer(i -> {
				Room r = i.getArgument(0);
				r.setId(2L);
				return r;
			});

			Optional<Room> result = roomService.create(newRoom, 1L);

			assertThat(result).isPresent();
			assertThat(result.get().getBuilding()).isEqualTo(testBuilding);
			verify(roomRepository).save(newRoom);
		}

		@Test
		@DisplayName("should return empty when building not found")
		void shouldReturnEmptyWhenBuildingNotFound() {
			Room newRoom = Room.builder()
					.roomNumber("201")
					.capacity(25)
					.type(Room.RoomType.SEMINAR)
					.build();

			when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

			Optional<Room> result = roomService.create(newRoom, 999L);

			assertThat(result).isEmpty();
			verify(roomRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("update")
	class Update {

		@Test
		@DisplayName("should update room when found")
		void shouldUpdateRoomWhenFound() {
			Room updated = Room.builder()
					.roomNumber("101A")
					.capacity(35)
					.type(Room.RoomType.LAB)
					.features("Computers, Projector")
					.build();

			when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
			when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

			Optional<Room> result = roomService.update(1L, updated);

			assertThat(result).isPresent();
			assertThat(result.get().getRoomNumber()).isEqualTo("101A");
			assertThat(result.get().getCapacity()).isEqualTo(35);
			assertThat(result.get().getType()).isEqualTo(Room.RoomType.LAB);
		}

		@Test
		@DisplayName("should return empty when room not found")
		void shouldReturnEmptyWhenRoomNotFound() {
			when(roomRepository.findById(999L)).thenReturn(Optional.empty());

			Optional<Room> result = roomService.update(999L, testRoom);

			assertThat(result).isEmpty();
			verify(roomRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("delete")
	class Delete {

		@Test
		@DisplayName("should return true when room is deleted")
		void shouldReturnTrueWhenRoomIsDeleted() {
			when(roomRepository.existsById(1L)).thenReturn(true);

			boolean result = roomService.delete(1L);

			assertThat(result).isTrue();
			verify(roomRepository).deleteById(1L);
		}

		@Test
		@DisplayName("should return false when room not found")
		void shouldReturnFalseWhenRoomNotFound() {
			when(roomRepository.existsById(999L)).thenReturn(false);

			boolean result = roomService.delete(999L);

			assertThat(result).isFalse();
			verify(roomRepository, never()).deleteById(any());
		}
	}
}
