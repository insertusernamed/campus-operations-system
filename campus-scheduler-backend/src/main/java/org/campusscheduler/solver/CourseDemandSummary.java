package org.campusscheduler.solver;

/**
 * Aggregated demand fact for a course derived from student request baskets.
 */
public record CourseDemandSummary(
        Long courseId,
        int totalRequestCount,
        int primaryRequestCount,
        int highPriorityRequestCount) {
}
