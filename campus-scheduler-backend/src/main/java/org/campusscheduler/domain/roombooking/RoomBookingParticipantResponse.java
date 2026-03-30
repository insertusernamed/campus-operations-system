package org.campusscheduler.domain.roombooking;

import org.campusscheduler.domain.student.Student;

/**
 * Visible participant details for a room booking.
 */
public record RoomBookingParticipantResponse(
        Long id,
        String fullName,
        String email) {

    public static RoomBookingParticipantResponse from(Student student) {
        if (student == null) {
            return null;
        }
        String firstName = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String lastName = student.getLastName() == null ? "" : student.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return new RoomBookingParticipantResponse(
                student.getId(),
                fullName,
                student.getEmail());
    }
}
