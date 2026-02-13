package org.campusscheduler.api.error;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error payload.
 *
 * <p>
 * The API historically returned {@code {"error": "..."} } for some endpoints.
 * For compatibility, both {@code error} and {@code message} are included.
 * </p>
 *
 * <p>
 * {@code timestamp} is generated server-side in UTC.
 * </p>
 */
public record ApiErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        int status,
        String code,
        String message,
        String error,
        String path,
        List<ApiFieldError> fieldErrors) {

    public static ApiErrorResponse of(
            int status,
            String code,
            String message,
            String path,
            List<ApiFieldError> fieldErrors) {
        String safeMessage = message == null ? "" : message;
        return new ApiErrorResponse(
                Instant.now(),
                status,
                code,
                safeMessage,
                safeMessage,
                path,
                fieldErrors == null ? List.of() : fieldErrors);
    }
}
