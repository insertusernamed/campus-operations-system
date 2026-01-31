package org.campusscheduler.solver;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Factory for creating Timefold Solvers with dynamic timeout based on problem size.
 * Uses SolverFactory directly instead of SolverManager to allow PhaseLifecycleListeners
 * for real-time progress updates during Construction Heuristic.
 */
@Component
public class SolverManagerConfiguration {

    /**
     * Calculate timeout based on problem size.
     * Base: 30s for up to 200 courses, scales up for larger problems.
     */
    public Duration calculateTimeout(int courseCount) {
        if (courseCount <= 200) {
            return Duration.ofSeconds(30);
        } else if (courseCount <= 500) {
            return Duration.ofSeconds(60);
        } else if (courseCount <= 1000) {
            return Duration.ofSeconds(120);
        } else {
            // 2 minutes + 1 minute per 500 courses over 1000
            long extraMinutes = (courseCount - 1000) / 500 + 1;
            return Duration.ofMinutes(2 + extraMinutes);
        }
    }

    /**
     * Build a SolverFactory with timeout appropriate for the given course count.
     */
    public SolverFactory<ScheduleSolution> createSolverFactory(int courseCount) {
        Duration timeout = calculateTimeout(courseCount);

        ConstructionHeuristicPhaseConfig chConfig = new ConstructionHeuristicPhaseConfig();
        LocalSearchPhaseConfig lsConfig = new LocalSearchPhaseConfig();

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(ScheduleSolution.class)
                .withEntityClasses(ScheduleAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSpentLimit(timeout));

        solverConfig.setPhaseConfigList(List.of(chConfig, lsConfig));

        return SolverFactory.create(solverConfig);
    }
}
