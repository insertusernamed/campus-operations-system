package org.campusscheduler.domain.changerequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for approving or rejecting a change request.
 */
@Data
public class ChangeRequestDecisionRequest {

    @Schema(description = "Optional note explaining the approval or rejection decision")
    @Size(max = 500, message = "Decision note must not exceed 500 characters")
    private String decisionNote;

    @Schema(description = "Override proposed room when approving the request")
    private Long proposedRoomId;

    @Schema(description = "Override proposed time slot when approving the request")
    private Long proposedTimeSlotId;
}
