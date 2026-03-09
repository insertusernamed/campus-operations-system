package org.campusscheduler.domain.instructorinsight;

/**
 * Weekly schedule density row grouped by day of week.
 */
public record InstructorWeeklyDensityResponse(
        String dayOfWeek,
        int classCount,
        long totalMinutes
) {
}
