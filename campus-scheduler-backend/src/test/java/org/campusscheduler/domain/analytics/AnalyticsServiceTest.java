package org.campusscheduler.domain.analytics;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Building testBuilding;
    private Room testRoom1;
    private Room testRoom2;
    private TimeSlot testTimeSlot1;
    private TimeSlot testTimeSlot2;
    private static final String TEST_SEMESTER = "Spring 2026";

    @BeforeEach
    void setUp() {
        testBuilding = Building.builder()
                .id(1L)
                .name("Science Building")
                .code("SCI")
                .build();

        testRoom1 = Room.builder()
                .id(1L)
                .roomNumber("101")
                .capacity(30)
                .type(Room.RoomType.CLASSROOM)
                .building(testBuilding)
                .build();

        testRoom2 = Room.builder()
                .id(2L)
                .roomNumber("102")
                .capacity(50)
                .type(Room.RoomType.LECTURE_HALL)
                .building(testBuilding)
                .build();

        testTimeSlot1 = TimeSlot.builder()
                .id(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Period 1")
                .build();

        testTimeSlot2 = TimeSlot.builder()
                .id(2L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 30))
                .label("Period 2")
                .build();
    }

    @Nested
    @DisplayName("getRoomUtilization")
    class GetRoomUtilization {

        @Test
        @DisplayName("should calculate room utilization correctly")
        void shouldCalculateRoomUtilizationCorrectly() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom1));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(5L);

            Optional<RoomUtilizationDTO> result = analyticsService.getRoomUtilization(1L, TEST_SEMESTER);

            assertThat(result).isPresent();
            assertThat(result.get().getRoomId()).isEqualTo(1L);
            assertThat(result.get().getRoomNumber()).isEqualTo("101");
            assertThat(result.get().getScheduledSlots()).isEqualTo(5L);
            assertThat(result.get().getTotalSlots()).isEqualTo(10L);
            assertThat(result.get().getUtilizationPercentage()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("should return empty when room not found")
        void shouldReturnEmptyWhenRoomNotFound() {
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<RoomUtilizationDTO> result = analyticsService.getRoomUtilization(999L, TEST_SEMESTER);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return 0% utilization when no schedules")
        void shouldReturnZeroUtilizationWhenNoSchedules() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom1));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(0L);

            Optional<RoomUtilizationDTO> result = analyticsService.getRoomUtilization(1L, TEST_SEMESTER);

            assertThat(result).isPresent();
            assertThat(result.get().getUtilizationPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle zero total slots without division error")
        void shouldHandleZeroTotalSlotsWithoutDivisionError() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom1));
            when(timeSlotRepository.count()).thenReturn(0L);

            Optional<RoomUtilizationDTO> result = analyticsService.getRoomUtilization(1L, TEST_SEMESTER);

            assertThat(result).isPresent();
            assertThat(result.get().getUtilizationPercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getAllRoomsUtilization")
    class GetAllRoomsUtilization {

        @Test
        @DisplayName("should return utilization for all rooms")
        void shouldReturnUtilizationForAllRooms() {
            when(roomRepository.findAll()).thenReturn(List.of(testRoom1, testRoom2));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(5L);
            when(scheduleRepository.countByRoomIdAndSemester(2L, TEST_SEMESTER)).thenReturn(8L);

            List<RoomUtilizationDTO> result = analyticsService.getAllRoomsUtilization(TEST_SEMESTER);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUtilizationPercentage()).isEqualTo(50.0);
            assertThat(result.get(1).getUtilizationPercentage()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("should return empty list when no rooms exist")
        void shouldReturnEmptyListWhenNoRoomsExist() {
            when(roomRepository.findAll()).thenReturn(List.of());

            List<RoomUtilizationDTO> result = analyticsService.getAllRoomsUtilization(TEST_SEMESTER);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBuildingUtilization")
    class GetBuildingUtilization {

        @Test
        @DisplayName("should calculate building utilization correctly")
        void shouldCalculateBuildingUtilizationCorrectly() {
            testBuilding.setRooms(List.of(testRoom1, testRoom2));
            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
            when(timeSlotRepository.count()).thenReturn(10L);
            // Optimized query mock
            when(scheduleRepository.countSchedulesByBuildingAndSemester(1L, TEST_SEMESTER)).thenReturn(13L);

            Optional<BuildingUtilizationDTO> result = analyticsService.getBuildingUtilization(1L, TEST_SEMESTER);

            assertThat(result).isPresent();
            assertThat(result.get().getBuildingId()).isEqualTo(1L);
            assertThat(result.get().getBuildingName()).isEqualTo("Science Building");
            assertThat(result.get().getRoomCount()).isEqualTo(2);
            assertThat(result.get().getScheduledSlots()).isEqualTo(13L); // 13 from optimized count
            assertThat(result.get().getTotalSlots()).isEqualTo(20L); // 2 rooms * 10 slots
            assertThat(result.get().getUtilizationPercentage()).isEqualTo(65.0);
        }

        @Test
        @DisplayName("should return empty when building not found")
        void shouldReturnEmptyWhenBuildingNotFound() {
            when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<BuildingUtilizationDTO> result = analyticsService.getBuildingUtilization(999L, TEST_SEMESTER);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllBuildingsUtilization")
    class GetAllBuildingsUtilization {

        @Test
        @DisplayName("should return utilization for all buildings")
        void shouldReturnUtilizationForAllBuildings() {
            testBuilding.setRooms(List.of(testRoom1));
            Building testBuilding2 = Building.builder()
                    .id(2L)
                    .name("Arts Building")
                    .code("ART")
                    .rooms(List.of(testRoom2))
                    .build();
            testRoom2.setBuilding(testBuilding2);

            when(buildingRepository.findAll()).thenReturn(List.of(testBuilding, testBuilding2));
            when(timeSlotRepository.count()).thenReturn(10L);
            // Optimized query mocks
            when(scheduleRepository.countSchedulesByBuildingAndSemester(1L, TEST_SEMESTER)).thenReturn(5L);
            when(scheduleRepository.countSchedulesByBuildingAndSemester(2L, TEST_SEMESTER)).thenReturn(8L);

            List<BuildingUtilizationDTO> result = analyticsService.getAllBuildingsUtilization(TEST_SEMESTER);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getPeakHours")
    class GetPeakHours {

        @Test
        @DisplayName("should return time slots sorted by booking count descending")
        void shouldReturnTimeSlotsSortedByBookingCountDescending() {
            when(timeSlotRepository.findAll()).thenReturn(List.of(testTimeSlot1, testTimeSlot2));
            when(scheduleRepository.countByTimeSlotIdAndSemester(1L, TEST_SEMESTER)).thenReturn(3L);
            when(scheduleRepository.countByTimeSlotIdAndSemester(2L, TEST_SEMESTER)).thenReturn(7L);

            List<PeakHoursDTO> result = analyticsService.getPeakHours(TEST_SEMESTER);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBookingCount()).isEqualTo(7L); // Higher count first
            assertThat(result.get(1).getBookingCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should return empty list when no time slots exist")
        void shouldReturnEmptyListWhenNoTimeSlotsExist() {
            when(timeSlotRepository.findAll()).thenReturn(List.of());

            List<PeakHoursDTO> result = analyticsService.getPeakHours(TEST_SEMESTER);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnderusedRooms")
    class GetUnderusedRooms {

        @Test
        @DisplayName("should return rooms below utilization threshold")
        void shouldReturnRoomsBelowUtilizationThreshold() {
            when(roomRepository.findAll()).thenReturn(List.of(testRoom1, testRoom2));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(2L); // 20%
            when(scheduleRepository.countByRoomIdAndSemester(2L, TEST_SEMESTER)).thenReturn(8L); // 80%

            List<RoomUtilizationDTO> result = analyticsService.getUnderusedRooms(TEST_SEMESTER, 50.0);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoomNumber()).isEqualTo("101");
            assertThat(result.get(0).getUtilizationPercentage()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("should return all rooms when all are below threshold")
        void shouldReturnAllRoomsWhenAllAreBelowThreshold() {
            when(roomRepository.findAll()).thenReturn(List.of(testRoom1, testRoom2));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(2L); // 20%
            when(scheduleRepository.countByRoomIdAndSemester(2L, TEST_SEMESTER)).thenReturn(3L); // 30%

            List<RoomUtilizationDTO> result = analyticsService.getUnderusedRooms(TEST_SEMESTER, 50.0);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when all rooms above threshold")
        void shouldReturnEmptyListWhenAllRoomsAboveThreshold() {
            when(roomRepository.findAll()).thenReturn(List.of(testRoom1, testRoom2));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(8L); // 80%
            when(scheduleRepository.countByRoomIdAndSemester(2L, TEST_SEMESTER)).thenReturn(9L); // 90%

            List<RoomUtilizationDTO> result = analyticsService.getUnderusedRooms(TEST_SEMESTER, 50.0);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUtilizationSummary")
    class GetUtilizationSummary {

        @Test
        @DisplayName("should return overall utilization summary")
        void shouldReturnOverallUtilizationSummary() {
            testBuilding.setRooms(List.of(testRoom1, testRoom2));
            when(roomRepository.findAll()).thenReturn(List.of(testRoom1, testRoom2));
            when(buildingRepository.findAll()).thenReturn(List.of(testBuilding));
            when(timeSlotRepository.count()).thenReturn(10L);
            when(scheduleRepository.countByRoomIdAndSemester(1L, TEST_SEMESTER)).thenReturn(5L);
            when(scheduleRepository.countByRoomIdAndSemester(2L, TEST_SEMESTER)).thenReturn(8L);

            UtilizationSummaryDTO result = analyticsService.getUtilizationSummary(TEST_SEMESTER);

            assertThat(result.getSemester()).isEqualTo(TEST_SEMESTER);
            assertThat(result.getTotalRooms()).isEqualTo(2);
            assertThat(result.getTotalBuildings()).isEqualTo(1);
            assertThat(result.getTotalScheduledSlots()).isEqualTo(13L);
            assertThat(result.getTotalAvailableSlots()).isEqualTo(20L);
            assertThat(result.getOverallUtilizationPercentage()).isEqualTo(65.0);
        }
    }
}
