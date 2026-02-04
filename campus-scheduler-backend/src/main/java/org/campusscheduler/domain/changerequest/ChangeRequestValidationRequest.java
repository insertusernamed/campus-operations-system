package org.campusscheduler.domain.changerequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for validating a proposed schedule change.
 */
@Data
public class ChangeRequestValidationRequest {

    @Schema(description = "Identifier of the existing schedule being validated")
    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @Schema(description = "Identifier of the proposed room, or null to keep the current room. At least one proposed field must be provided.")
    private Long proposedRoomId;

    @Schema(description = "Identifier of the proposed time slot, or null to keep the current time slot. At least one proposed field must be provided.")
    private Long proposedTimeSlotId;
}
