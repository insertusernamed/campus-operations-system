package org.campusscheduler.domain.roombooking;

/**
 * Minimal student payload for room booking participant autocomplete.
 */
public record RoomBookingStudentLookupResponse(
        Long id,
        String email,
        String fullName,
        boolean hasClassDuringPeriod) {
}
