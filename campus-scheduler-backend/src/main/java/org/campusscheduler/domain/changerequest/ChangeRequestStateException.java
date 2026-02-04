package org.campusscheduler.domain.changerequest;

/**
 * Thrown when a change request cannot be updated due to its current state.
 */
public class ChangeRequestStateException extends RuntimeException {

    public ChangeRequestStateException(String message) {
        super(message);
    }
}
