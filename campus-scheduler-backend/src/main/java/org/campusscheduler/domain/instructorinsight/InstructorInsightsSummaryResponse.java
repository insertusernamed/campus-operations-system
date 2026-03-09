package org.campusscheduler.domain.instructorinsight;

/**
 * Snapshot counts for the instructor operations dashboard.
 */
public record InstructorInsightsSummaryResponse(
        long totalInstructors,
        long noCurrentAssignment,
        long overloadRisk,
        long preferenceSetupIncomplete,
        long frictionHotspots,
        long departmentsWithCoverageRisk
) {
}
