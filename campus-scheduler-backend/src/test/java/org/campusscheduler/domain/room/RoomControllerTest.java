package org.campusscheduler.domain.room;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.domain.building.Building;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for RoomController using MockMvc.
 */
@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
class RoomControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
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
				.features("Projector")
				.building(testBuilding)
				.build();
	}

	@Nested
	@DisplayName("GET /api/rooms")
	class GetAllRooms {

		@Test
		@DisplayName("should return all rooms")
		void shouldReturnAllRooms() throws Exception {
			when(roomService.findAll()).thenReturn(List.of(testRoom));

			mockMvc.perform(get("/api/rooms"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].roomNumber", is("101")));
		}

		@Test
		@DisplayName("should filter rooms by building ID")
		void shouldFilterRoomsByBuildingId() throws Exception {
			when(roomService.findByBuildingId(1L)).thenReturn(List.of(testRoom));

			mockMvc.perform(get("/api/rooms").param("buildingId", "1"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)));
		}
	}

	@Nested
	@DisplayName("GET /api/rooms/{id}")
	class GetRoomById {

		@Test
		@DisplayName("should return room when found")
		void shouldReturnRoomWhenFound() throws Exception {
			when(roomService.findById(1L)).thenReturn(Optional.of(testRoom));

			mockMvc.perform(get("/api/rooms/1"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id", is(1)))
					.andExpect(jsonPath("$.roomNumber", is("101")))
					.andExpect(jsonPath("$.capacity", is(30)));
		}

		@Test
		@DisplayName("should return 404 when not found")
		void shouldReturn404WhenNotFound() throws Exception {
			when(roomService.findById(999L)).thenReturn(Optional.empty());

			mockMvc.perform(get("/api/rooms/999"))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("GET /api/rooms/type/{type}")
	class GetRoomsByType {

		@Test
		@DisplayName("should return rooms of specified type")
		void shouldReturnRoomsOfSpecifiedType() throws Exception {
			when(roomService.findByType(Room.RoomType.CLASSROOM)).thenReturn(List.of(testRoom));

			mockMvc.perform(get("/api/rooms/type/CLASSROOM"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].type", is("CLASSROOM")));
		}
	}

	@Nested
	@DisplayName("GET /api/rooms/capacity/{capacity}")
	class GetRoomsByCapacity {

		@Test
		@DisplayName("should return rooms with minimum capacity")
		void shouldReturnRoomsWithMinimumCapacity() throws Exception {
			when(roomService.findByMinCapacity(25)).thenReturn(List.of(testRoom));

			mockMvc.perform(get("/api/rooms/capacity/25"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)));
		}
	}

	@Nested
	@DisplayName("POST /api/rooms/building/{buildingId}")
	class CreateRoom {

		@Test
		@DisplayName("should create room and return 201")
		void shouldCreateRoomAndReturn201() throws Exception {
			Room savedRoom = Room.builder()
					.id(2L)
					.roomNumber("201")
					.capacity(25)
					.type(Room.RoomType.SEMINAR)
					.building(testBuilding)
					.build();

			when(roomService.create(any(Room.class), eq(1L))).thenReturn(Optional.of(savedRoom));

			mockMvc.perform(post("/api/rooms/building/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"roomNumber\":\"201\",\"capacity\":25,\"type\":\"SEMINAR\"}"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.id", is(2)))
					.andExpect(jsonPath("$.roomNumber", is("201")));
		}

		@Test
		@DisplayName("should return 404 when building not found")
		void shouldReturn404WhenBuildingNotFound() throws Exception {
			when(roomService.create(any(Room.class), eq(999L))).thenReturn(Optional.empty());

			mockMvc.perform(post("/api/rooms/building/999")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"roomNumber\":\"201\",\"capacity\":25,\"type\":\"SEMINAR\"}"))
					.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("should return 400 when room number is blank")
		void shouldReturn400WhenRoomNumberIsBlank() throws Exception {
			mockMvc.perform(post("/api/rooms/building/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"roomNumber\":\"\",\"capacity\":25,\"type\":\"SEMINAR\"}"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("should return 400 when capacity is less than 1")
		void shouldReturn400WhenCapacityIsInvalid() throws Exception {
			mockMvc.perform(post("/api/rooms/building/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"roomNumber\":\"201\",\"capacity\":0,\"type\":\"SEMINAR\"}"))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("PUT /api/rooms/{id}")
	class UpdateRoom {

		@Test
		@DisplayName("should update room when found")
		void shouldUpdateRoomWhenFound() throws Exception {
			Room updated = Room.builder()
					.id(1L)
					.roomNumber("101A")
					.capacity(35)
					.type(Room.RoomType.LAB)
					.build();

			when(roomService.update(eq(1L), any(Room.class))).thenReturn(Optional.of(updated));

			mockMvc.perform(put("/api/rooms/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"roomNumber\":\"101A\",\"capacity\":35,\"type\":\"LAB\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.roomNumber", is("101A")));
		}

		@Test
		@DisplayName("should return 404 when room not found")
		void shouldReturn404WhenRoomNotFound() throws Exception {
			when(roomService.update(eq(999L), any(Room.class))).thenReturn(Optional.empty());

			mockMvc.perform(put("/api/rooms/999")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"roomNumber\":\"101\",\"capacity\":30,\"type\":\"CLASSROOM\"}"))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("DELETE /api/rooms/{id}")
	class DeleteRoom {

		@Test
		@DisplayName("should return 204 when deleted")
		void shouldReturn204WhenDeleted() throws Exception {
			when(roomService.delete(1L)).thenReturn(true);

			mockMvc.perform(delete("/api/rooms/1"))
					.andExpect(status().isNoContent());
		}

		@Test
		@DisplayName("should return 404 when not found")
		void shouldReturn404WhenNotFound() throws Exception {
			when(roomService.delete(999L)).thenReturn(false);

			mockMvc.perform(delete("/api/rooms/999"))
					.andExpect(status().isNotFound());
		}
	}
}
