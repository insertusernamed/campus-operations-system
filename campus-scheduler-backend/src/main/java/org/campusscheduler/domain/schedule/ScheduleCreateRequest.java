package org.campusscheduler.domain.schedule;

import lombok.Data;

/**
 * Request DTO for creating a schedule.
 */
@Data
public class ScheduleCreateRequest {
    private Long courseId;
    private Long roomId;
    private Long timeSlotId;
    private String semester;
}
