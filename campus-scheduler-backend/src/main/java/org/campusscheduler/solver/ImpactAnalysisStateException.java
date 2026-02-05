package org.campusscheduler.solver;

/**
 * Thrown when an impact analysis request is invalid or incomplete.
 */
public class ImpactAnalysisStateException extends RuntimeException {

    public ImpactAnalysisStateException(String message) {
        super(message);
    }
}
