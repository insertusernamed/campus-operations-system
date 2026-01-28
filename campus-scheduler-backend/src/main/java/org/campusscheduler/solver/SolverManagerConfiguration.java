package org.campusscheduler.solver;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Manual configuration for Timefold Solver.
 * Uses SolverFactory directly instead of SolverManager to allow PhaseLifecycleListeners
 * for real-time progress updates during Construction Heuristic.
 */
@Configuration
public class SolverManagerConfiguration {

    @Bean
    public SolverFactory<ScheduleSolution> solverFactory() {
        // Create phase configs
        ConstructionHeuristicPhaseConfig chConfig = new ConstructionHeuristicPhaseConfig();
        LocalSearchPhaseConfig lsConfig = new LocalSearchPhaseConfig();

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(ScheduleSolution.class)
                .withEntityClasses(ScheduleAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSpentLimit(Duration.ofSeconds(30)));

        // Set phase config list using setter method
        solverConfig.setPhaseConfigList(List.of(chConfig, lsConfig));

        return SolverFactory.create(solverConfig);
    }
}
