package org.campusscheduler.domain.schedule;

/**
 * Exception thrown when a scheduling conflict is detected.
 */
public class ScheduleConflictException extends RuntimeException {

    public ScheduleConflictException(String message) {
        super(message);
    }
}
