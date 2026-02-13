package org.campusscheduler.api.error;

/**
 * Field-level validation error for API responses.
 */
public record ApiFieldError(
        String field,
        String message) {
}

