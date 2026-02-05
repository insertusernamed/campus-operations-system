package org.campusscheduler.solver;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a schedule move suggested by impact analysis.
 */
@Data
@Builder
public class ImpactAnalysisMove {

    private Long scheduleId;
    private String courseCode;

    private Long fromRoomId;
    private String fromRoomLabel;

    private Long toRoomId;
    private String toRoomLabel;

    private Long fromTimeSlotId;
    private String fromTimeSlotLabel;

    private Long toTimeSlotId;
    private String toTimeSlotLabel;
}
