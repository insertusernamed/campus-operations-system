package org.campusscheduler.domain.roombooking;

/**
 * Exception thrown when a student room booking conflicts with policy or usage.
 */
public class RoomBookingConflictException extends RuntimeException {

    public RoomBookingConflictException(String message) {
        super(message);
    }
}
