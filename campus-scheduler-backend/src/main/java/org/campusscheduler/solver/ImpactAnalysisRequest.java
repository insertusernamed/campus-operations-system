package org.campusscheduler.solver;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for impact analysis.
 */
@Data
public class ImpactAnalysisRequest {

    @Schema(description = "Identifier of the schedule to change")
    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @Schema(description = "Identifier of the proposed room, or null to keep current. At least one proposed field must be provided.")
    private Long proposedRoomId;

    @Schema(description = "Identifier of the proposed time slot, or null to keep current. At least one proposed field must be provided.")
    private Long proposedTimeSlotId;
}
