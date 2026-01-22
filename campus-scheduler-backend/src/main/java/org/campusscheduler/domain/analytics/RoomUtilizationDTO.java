package org.campusscheduler.domain.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for room utilization statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUtilizationDTO {

    private Long roomId;
    private String roomNumber;
    private String buildingName;
    private String buildingCode;
    private int capacity;
    private long scheduledSlots;
    private long totalSlots;
    private double utilizationPercentage;
}
