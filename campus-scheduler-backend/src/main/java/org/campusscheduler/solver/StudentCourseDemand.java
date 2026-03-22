package org.campusscheduler.solver;

/**
 * Immutable demand fact describing one student's ranked request for a course.
 */
public record StudentCourseDemand(
        Long studentId,
        Long courseId,
        int preferenceRank,
        int targetCourseLoad,
        boolean primaryRequest,
        boolean highPriorityRequest) {
}
