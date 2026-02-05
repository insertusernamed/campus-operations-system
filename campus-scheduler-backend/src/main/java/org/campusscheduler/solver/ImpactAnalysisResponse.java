package org.campusscheduler.solver;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for impact analysis.
 */
@Data
@Builder
public class ImpactAnalysisResponse {

    public enum Status {
        SOLVED,
        NO_SOLUTION
    }

    private Status status;
    private String score;
    private String scoreSummary;
    private List<ImpactAnalysisMove> moves;
    private List<ImpactConstraintSummary> constraintSummaries;
}
