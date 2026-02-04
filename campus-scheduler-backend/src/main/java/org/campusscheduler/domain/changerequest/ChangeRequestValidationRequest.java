package org.campusscheduler.domain.changerequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for validating a proposed schedule change.
 */
@Data
public class ChangeRequestValidationRequest {

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    private Long proposedRoomId;

    private Long proposedTimeSlotId;
}
