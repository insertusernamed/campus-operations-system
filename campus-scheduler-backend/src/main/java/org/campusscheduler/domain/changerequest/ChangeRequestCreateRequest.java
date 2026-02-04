package org.campusscheduler.domain.changerequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a schedule change request.
 */
@Data
public class ChangeRequestCreateRequest {

    @Schema(description = "Identifier of the schedule for which the change is requested")
    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @Schema(description = "Identifier of the instructor submitting the change request")
    @NotNull(message = "Requested by instructor ID is required")
    private Long requestedByInstructorId;

    @Schema(description = "Role of the requester submitting the change")
    @NotNull(message = "Requested by role is required")
    private ChangeRequestRole requestedByRole;

    @Schema(description = "High-level category describing the reason for the change")
    @NotNull(message = "Reason category is required")
    private ChangeRequestReason reasonCategory;

    @Schema(description = "Additional details explaining the requested change")
    @Size(max = 500, message = "Reason details must not exceed 500 characters")
    private String reasonDetails;

    @Schema(description = "Identifier of the proposed room, if a room change is requested. At least one proposed field must be provided.")
    private Long proposedRoomId;

    @Schema(description = "Identifier of the proposed time slot, if a time change is requested. At least one proposed field must be provided.")
    private Long proposedTimeSlotId;
}
