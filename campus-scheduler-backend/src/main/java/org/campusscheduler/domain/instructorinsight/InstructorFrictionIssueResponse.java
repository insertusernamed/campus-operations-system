package org.campusscheduler.domain.instructorinsight;

/**
 * Instructor friction issue payload.
 */
public record InstructorFrictionIssueResponse(
        String id,
        InstructorFrictionType type,
        InstructorFrictionSeverity severity,
        Long scheduleId,
        String message,
        RecommendedIssue recommendedIssue
) {
}
