package org.campusscheduler.solver;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

/**
 * Immutable demand fact describing one student's ranked request for a course.
 */
public record StudentCourseDemand(
        @PlanningId Long id,
        Long studentId,
        Long courseId,
        int preferenceRank,
        int targetCourseLoad,
        boolean primaryRequest,
        boolean highPriorityRequest) {
}
