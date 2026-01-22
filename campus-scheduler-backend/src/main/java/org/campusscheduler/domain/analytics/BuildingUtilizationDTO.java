package org.campusscheduler.domain.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for building utilization statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingUtilizationDTO {

    private Long buildingId;
    private String buildingName;
    private String buildingCode;
    private int roomCount;
    private long scheduledSlots;
    private long totalSlots;
    private double utilizationPercentage;
}
