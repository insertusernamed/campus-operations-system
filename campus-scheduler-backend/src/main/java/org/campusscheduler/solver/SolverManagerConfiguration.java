package org.campusscheduler.solver;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${solver.unimproved-limit:10s}")
    private Duration unimprovedLimit = Duration.ofSeconds(10);

    @Value("${demo.solver.seed:#{null}}")
    private Long solverSeed;

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
     * Stop solving if score has not improved for this duration.
     *
     * <p>
     * This prevents running the full spent-limit when the search has plateaued.
     * </p>
     */
    public Duration calculateUnimprovedTimeout() {
        return unimprovedLimit;
    }

    /**
     * Build a SolverFactory with timeout appropriate for the given course count.
     */
    public SolverFactory<ScheduleSolution> createSolverFactory(int courseCount) {
        Duration timeout = calculateTimeout(courseCount);
        Duration unimprovedTimeout = calculateUnimprovedTimeout();

        ConstructionHeuristicPhaseConfig chConfig = new ConstructionHeuristicPhaseConfig()
                .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT_DECREASING)
                .withEntitySorterManner(EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE);

        LocalSearchPhaseConfig lsConfig = new LocalSearchPhaseConfig()
                .withLocalSearchType(LocalSearchType.LATE_ACCEPTANCE);

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(ScheduleSolution.class)
                .withEntityClasses(ScheduleAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSpentLimit(timeout)
                                .withUnimprovedSpentLimit(unimprovedTimeout));
        if (solverSeed != null) {
            solverConfig.setRandomSeed(solverSeed);
        }

        solverConfig.setPhaseConfigList(List.of(chConfig, lsConfig));

        return SolverFactory.create(solverConfig);
    }

    /**
     * Build a SolverFactory for impact analysis with a short timeout, suitable
     * for use with {@code SolutionManager.explain()} to obtain constraint-match
     * based explanations.
     */
    public SolverFactory<ScheduleSolution> createImpactSolverFactory() {
        Duration timeout = Duration.ofSeconds(5);

        ConstructionHeuristicPhaseConfig chConfig = new ConstructionHeuristicPhaseConfig()
                .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT);

        LocalSearchPhaseConfig lsConfig = new LocalSearchPhaseConfig()
                .withLocalSearchType(LocalSearchType.HILL_CLIMBING);

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(ScheduleSolution.class)
                .withEntityClasses(ScheduleAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSpentLimit(timeout));
        if (solverSeed != null) {
            solverConfig.setRandomSeed(solverSeed);
        }

        solverConfig.setPhaseConfigList(List.of(chConfig, lsConfig));

        return SolverFactory.create(solverConfig);
    }
}
