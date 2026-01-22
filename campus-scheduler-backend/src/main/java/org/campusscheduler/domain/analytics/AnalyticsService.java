package org.campusscheduler.domain.analytics;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for analytics and utilization calculations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ScheduleRepository scheduleRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final TimeSlotRepository timeSlotRepository;

    /**
     * Get utilization statistics for a specific room.
     *
     * @param roomId   the room ID
     * @param semester the semester to filter by
     * @return room utilization DTO, or empty if room not found
     */
    public Optional<RoomUtilizationDTO> getRoomUtilization(Long roomId, String semester) {
        return roomRepository.findById(roomId)
                .map(room -> buildRoomUtilizationDTO(room, semester));
    }

    /**
     * Get utilization statistics for all rooms.
     *
     * @param semester the semester to filter by
     * @return list of room utilization DTOs
     */
    public List<RoomUtilizationDTO> getAllRoomsUtilization(String semester) {
        return roomRepository.findAll().stream()
                .map(room -> buildRoomUtilizationDTO(room, semester))
                .collect(Collectors.toList());
    }

    /**
     * Get utilization statistics for a specific building.
     *
     * @param buildingId the building ID
     * @param semester   the semester to filter by
     * @return building utilization DTO, or empty if building not found
     */
    public Optional<BuildingUtilizationDTO> getBuildingUtilization(Long buildingId, String semester) {
        return buildingRepository.findById(buildingId)
                .map(building -> buildBuildingUtilizationDTO(building, semester));
    }

    /**
     * Get utilization statistics for all buildings.
     *
     * @param semester the semester to filter by
     * @return list of building utilization DTOs
     */
    public List<BuildingUtilizationDTO> getAllBuildingsUtilization(String semester) {
        return buildingRepository.findAll().stream()
                .map(building -> buildBuildingUtilizationDTO(building, semester))
                .collect(Collectors.toList());
    }

    /**
     * Get peak hours sorted by booking count descending.
     *
     * @param semester the semester to filter by
     * @return list of peak hours DTOs sorted by booking count
     */
    public List<PeakHoursDTO> getPeakHours(String semester) {
        return timeSlotRepository.findAll().stream()
                .map(timeSlot -> buildPeakHoursDTO(timeSlot, semester))
                .sorted(Comparator.comparing(PeakHoursDTO::getBookingCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get rooms that are underused (below the utilization threshold).
     *
     * @param semester  the semester to filter by
     * @param threshold the utilization percentage threshold
     * @return list of underused room DTOs
     */
    public List<RoomUtilizationDTO> getUnderusedRooms(String semester, double threshold) {
        return getAllRoomsUtilization(semester).stream()
                .filter(room -> room.getUtilizationPercentage() < threshold)
                .sorted(Comparator.comparing(RoomUtilizationDTO::getUtilizationPercentage))
                .collect(Collectors.toList());
    }

    /**
     * Get overall utilization summary for the campus.
     *
     * @param semester the semester to filter by
     * @return utilization summary DTO
     */
    public UtilizationSummaryDTO getUtilizationSummary(String semester) {
        List<RoomUtilizationDTO> allRooms = getAllRoomsUtilization(semester);
        List<Building> allBuildings = buildingRepository.findAll();

        long totalScheduled = allRooms.stream()
                .mapToLong(RoomUtilizationDTO::getScheduledSlots)
                .sum();
        long totalAvailable = allRooms.stream()
                .mapToLong(RoomUtilizationDTO::getTotalSlots)
                .sum();

        double overallUtilization = totalAvailable > 0
                ? (double) totalScheduled / totalAvailable * 100
                : 0.0;

        List<RoomUtilizationDTO> sortedByUtilization = allRooms.stream()
                .sorted(Comparator.comparing(RoomUtilizationDTO::getUtilizationPercentage).reversed())
                .collect(Collectors.toList());

        List<RoomUtilizationDTO> topUtilized = sortedByUtilization.stream()
                .limit(5)
                .collect(Collectors.toList());

        List<RoomUtilizationDTO> leastUtilized = allRooms.stream()
                .sorted(Comparator.comparing(RoomUtilizationDTO::getUtilizationPercentage))
                .limit(5)
                .collect(Collectors.toList());

        return UtilizationSummaryDTO.builder()
                .semester(semester)
                .totalRooms(allRooms.size())
                .totalBuildings(allBuildings.size())
                .totalScheduledSlots(totalScheduled)
                .totalAvailableSlots(totalAvailable)
                .overallUtilizationPercentage(overallUtilization)
                .topUtilizedRooms(topUtilized)
                .leastUtilizedRooms(leastUtilized)
                .build();
    }

    /**
     * Build a RoomUtilizationDTO for a given room.
     */
    private RoomUtilizationDTO buildRoomUtilizationDTO(Room room, String semester) {
        long totalSlots = timeSlotRepository.count();
        long scheduledSlots = scheduleRepository.countByRoomIdAndSemester(room.getId(), semester);
        double utilization = totalSlots > 0
                ? (double) scheduledSlots / totalSlots * 100
                : 0.0;

        Building building = room.getBuilding();
        return RoomUtilizationDTO.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .buildingName(building != null ? building.getName() : null)
                .buildingCode(building != null ? building.getCode() : null)
                .capacity(room.getCapacity())
                .scheduledSlots(scheduledSlots)
                .totalSlots(totalSlots)
                .utilizationPercentage(utilization)
                .build();
    }

    /**
     * Build a BuildingUtilizationDTO for a given building.
     */
    private BuildingUtilizationDTO buildBuildingUtilizationDTO(Building building, String semester) {
        List<Room> rooms = building.getRooms();
        long totalSlots = timeSlotRepository.count();

        long scheduledSlots = rooms.stream()
                .mapToLong(room -> scheduleRepository.countByRoomIdAndSemester(room.getId(), semester))
                .sum();
        long totalAvailableSlots = rooms.size() * totalSlots;

        double utilization = totalAvailableSlots > 0
                ? (double) scheduledSlots / totalAvailableSlots * 100
                : 0.0;

        return BuildingUtilizationDTO.builder()
                .buildingId(building.getId())
                .buildingName(building.getName())
                .buildingCode(building.getCode())
                .roomCount(rooms.size())
                .scheduledSlots(scheduledSlots)
                .totalSlots(totalAvailableSlots)
                .utilizationPercentage(utilization)
                .build();
    }

    /**
     * Build a PeakHoursDTO for a given time slot.
     */
    private PeakHoursDTO buildPeakHoursDTO(TimeSlot timeSlot, String semester) {
        long bookingCount = scheduleRepository.countByTimeSlotIdAndSemester(timeSlot.getId(), semester);

        return PeakHoursDTO.builder()
                .timeSlotId(timeSlot.getId())
                .dayOfWeek(timeSlot.getDayOfWeek())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
                .label(timeSlot.getLabel())
                .bookingCount(bookingCount)
                .build();
    }
}
