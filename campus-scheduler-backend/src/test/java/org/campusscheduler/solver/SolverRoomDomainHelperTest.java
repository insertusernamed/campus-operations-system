package org.campusscheduler.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolverRoomDomainHelperTest {

    @Test
    @DisplayName("should prioritize department-aligned buildings for computer science courses")
    void shouldPrioritizeDepartmentAlignedBuildings() {
        Course course = Course.builder()
                .id(1L)
                .department("Computer Science")
                .enrollmentCapacity(35)
                .build();

        Room engRoom = room(1L, "101", 40, Room.RoomType.CLASSROOM, "ENG");
        Room cscRoom = room(2L, "201", 45, Room.RoomType.CLASSROOM, "CSC");
        Room artRoom = room(3L, "301", 45, Room.RoomType.CLASSROOM, "ART");

        SolverRoomDomainHelper.RoomDomain domain = SolverRoomDomainHelper.buildRoomDomain(
                course,
                List.of(artRoom, engRoom, cscRoom));

        assertThat(domain.preferredBuildingCodes()).contains("CSC", "ENG");
        assertThat(domain.allowedRooms()).isNotEmpty();
        assertThat(domain.allowedRooms().getFirst().getBuildingCode()).isIn("CSC", "ENG");
    }

    @Test
    @DisplayName("should prefer lab rooms for lab-heavy departments when available")
    void shouldPreferLabsForLabHeavyDepartments() {
        Course course = Course.builder()
                .id(1L)
                .department("Chemistry")
                .enrollmentCapacity(20)
                .build();

        Room labRoom = room(1L, "LAB-1", 24, Room.RoomType.LAB, "CHM");
        Room classroom = room(2L, "101", 40, Room.RoomType.CLASSROOM, "SCI");

        SolverRoomDomainHelper.RoomDomain domain = SolverRoomDomainHelper.buildRoomDomain(
                course,
                List.of(classroom, labRoom));

        assertThat(domain.allowedRooms())
                .extracting(Room::getType)
                .containsOnly(Room.RoomType.LAB);
    }

    @Test
    @DisplayName("should fall back to all rooms when capacity filters remove all candidates")
    void shouldFallbackWhenNoCapacityMatch() {
        Course course = Course.builder()
                .id(1L)
                .department("Business")
                .enrollmentCapacity(500)
                .build();

        Room roomA = room(1L, "101", 30, Room.RoomType.CLASSROOM, "BUS");
        Room roomB = room(2L, "102", 35, Room.RoomType.CLASSROOM, "ENG");

        SolverRoomDomainHelper.RoomDomain domain = SolverRoomDomainHelper.buildRoomDomain(
                course,
                List.of(roomA, roomB));

        assertThat(domain.allowedRooms())
                .extracting(Room::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("should exclude unavailable rooms from allowed domain")
    void shouldExcludeUnavailableRooms() {
        Course course = Course.builder()
                .id(1L)
                .department("Computer Science")
                .enrollmentCapacity(30)
                .build();

        Room available = room(1L, "101", 40, Room.RoomType.CLASSROOM, "ENG");
        Room maintenance = room(2L, "102", 40, Room.RoomType.CLASSROOM, "ENG");
        maintenance.setAvailabilityStatus(Room.AvailabilityStatus.MAINTENANCE);
        Room outOfService = room(3L, "103", 40, Room.RoomType.CLASSROOM, "ENG");
        outOfService.setAvailabilityStatus(Room.AvailabilityStatus.OUT_OF_SERVICE);

        SolverRoomDomainHelper.RoomDomain domain = SolverRoomDomainHelper.buildRoomDomain(
                course,
                List.of(available, maintenance, outOfService));

        assertThat(domain.allowedRooms())
                .extracting(Room::getId)
                .containsExactly(1L);
    }

    private static Room room(Long id, String number, int capacity, Room.RoomType type, String buildingCode) {
        Room room = Room.builder()
                .id(id)
                .roomNumber(number)
                .capacity(capacity)
                .type(type)
                .build();
        var building = org.campusscheduler.domain.building.Building.builder()
                .id(id)
                .code(buildingCode)
                .name(buildingCode + " Building")
                .build();
        room.setBuilding(building);
        return room;
    }
}
