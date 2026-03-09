package org.campusscheduler.domain.instructorinsight;

/**
 * Friction issue counts grouped by severity.
 */
public record InstructorFrictionSummaryResponse(
        int total,
        int high,
        int medium,
        int low
) {
}
