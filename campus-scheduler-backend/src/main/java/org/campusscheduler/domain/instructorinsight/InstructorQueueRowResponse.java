package org.campusscheduler.domain.instructorinsight;

import java.util.List;

/**
 * Prioritized instructor queue row for admin triage workflows.
 */
public record InstructorQueueRowResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String department,
        int assignedCoursesCount,
        int assignedCredits,
        int targetCreditsMin,
        int targetCreditsMax,
        InstructorLoadStatus loadStatus,
        int preferenceCompletenessPercent,
        int frictionScore,
        int frictionIssueCount,
        InstructorFrictionSeverity frictionSeverity,
        InstructorCoverageRiskLevel coverageRiskLevel,
        InstructorOperationalStatus status,
        int overloadCredits,
        int underUtilizedCredits,
        List<String> recommendedActions
) {
}
