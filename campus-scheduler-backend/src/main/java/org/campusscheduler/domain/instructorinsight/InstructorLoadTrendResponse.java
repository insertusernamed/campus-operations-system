package org.campusscheduler.domain.instructorinsight;

/**
 * Current-term load compared with historical baseline.
 */
public record InstructorLoadTrendResponse(
        int currentCredits,
        double baselineCredits,
        double deltaCredits,
        String direction
) {
}
