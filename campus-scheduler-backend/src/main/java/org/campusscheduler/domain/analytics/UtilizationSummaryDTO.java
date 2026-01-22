package org.campusscheduler.domain.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for overall utilization summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilizationSummaryDTO {

    private String semester;
    private int totalRooms;
    private int totalBuildings;
    private long totalScheduledSlots;
    private long totalAvailableSlots;
    private double overallUtilizationPercentage;
    private List<RoomUtilizationDTO> topUtilizedRooms;
    private List<RoomUtilizationDTO> leastUtilizedRooms;
}
