package org.campusscheduler.domain.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO for peak hours statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeakHoursDTO {

    private Long timeSlotId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String label;
    private long bookingCount;
}
