package org.campusscheduler.domain.instructorinsight;

import java.util.List;

/**
 * Detailed instructor operational view for admin triage and planning.
 */
public record InstructorWorkbenchResponse(
        Long instructorId,
        String firstName,
        String lastName,
        String email,
        String department,
        String officeNumber,
        String semester,
        int assignedCoursesCount,
        int assignedCredits,
        int targetCreditsMin,
        int targetCreditsMax,
        InstructorLoadStatus loadStatus,
        int preferenceCompletenessPercent,
        int frictionScore,
        InstructorFrictionSummaryResponse frictionSummary,
        InstructorLoadTrendResponse loadTrend,
        List<InstructorWeeklyDensityResponse> weeklyDensity,
        List<InstructorAssignedCourseContextResponse> assignedCourses,
        List<InstructorFrictionIssueResponse> frictionIssues,
        List<InstructorRecentChangeResponse> recentChanges,
        List<String> recommendedActions
) {
}
