package org.campusscheduler.domain.roombooking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for creating a student room booking.
 */
@Getter
@Setter
public class CreateRoomBookingRequest {

    @NotNull(message = "Student is required")
    private Long studentId;

    @NotNull(message = "Room is required")
    private Long roomId;

    @NotNull(message = "Time slot is required")
    private Long timeSlotId;

    @NotBlank(message = "Semester is required")
    @Size(max = 50, message = "Semester must not exceed 50 characters")
    private String semester;

    @Size(max = 20, message = "A booking may include at most 20 invited students")
    private List<@NotBlank(message = "Participant email is required")
    @Email(message = "Participant email must be valid")
    @Size(max = 100, message = "Participant email must not exceed 100 characters") String> participantEmails = new ArrayList<>();
}
