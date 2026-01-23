package org.campusscheduler.websocket;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;

/**
 * Event sent to clients via WebSocket when solver progress updates.
 */
public record SolverProgressEvent(
        SolverStatus status,
        String score,
        int assignedCourses,
        int totalCourses,
        long hardViolations,
        long softScore,
        String message) {

    /**
     * Create from solver data.
     */
    public static SolverProgressEvent of(
            SolverStatus status,
            HardSoftScore score,
            int assignedCourses,
            int totalCourses,
            String message) {

        return new SolverProgressEvent(
                status,
                score != null ? score.toString() : null,
                assignedCourses,
                totalCourses,
                score != null ? -score.hardScore() : 0,
                score != null ? score.softScore() : 0,
                message);
    }
}
