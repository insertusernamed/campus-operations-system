package org.campusscheduler.domain.instructorinsight;

/**
 * Recent assignment-related change snippet for instructor workbench.
 */
public record InstructorRecentChangeResponse(
        String timestamp,
        String label,
        String source
) {
}
