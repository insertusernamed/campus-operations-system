package org.campusscheduler.domain.changerequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for approving or rejecting a change request.
 */
@Data
public class ChangeRequestDecisionRequest {

    @Size(max = 500, message = "Decision note must not exceed 500 characters")
    private String decisionNote;

    private Long proposedRoomId;

    private Long proposedTimeSlotId;
}
