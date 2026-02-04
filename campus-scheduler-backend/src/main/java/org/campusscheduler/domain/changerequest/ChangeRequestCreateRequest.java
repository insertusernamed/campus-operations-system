package org.campusscheduler.domain.changerequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a schedule change request.
 */
@Data
public class ChangeRequestCreateRequest {

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotNull(message = "Requested by instructor ID is required")
    private Long requestedByInstructorId;

    @NotNull(message = "Requested by role is required")
    private ChangeRequestRole requestedByRole;

    @NotNull(message = "Reason category is required")
    private ChangeRequestReason reasonCategory;

    @Size(max = 500, message = "Reason details must not exceed 500 characters")
    private String reasonDetails;

    private Long proposedRoomId;

    private Long proposedTimeSlotId;
}
