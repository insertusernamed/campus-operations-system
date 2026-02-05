package org.campusscheduler.solver;

import lombok.Builder;
import lombok.Data;

/**
 * Summary of constraint match totals for explanations.
 */
@Data
@Builder
public class ImpactConstraintSummary {

    private String constraintName;
    private String constraintId;
    private String score;
}
