package org.campusscheduler.domain.instructorinsight;

/**
 * Department-level load and coverage metrics.
 */
public record InstructorDepartmentLoadResponse(
        String department,
        int instructorCount,
        int assignedCredits,
        int targetCreditsMin,
        int targetCreditsMax,
        int unfilledCourseCount,
        int unfilledCredits,
        InstructorCoverageRiskLevel coverageRiskLevel
) {
}
