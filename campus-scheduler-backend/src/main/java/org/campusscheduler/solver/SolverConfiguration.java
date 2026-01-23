package org.campusscheduler.solver;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Manual configuration for Timefold Solver.
 * Required because timefold-solver-spring-boot-starter doesn't support Spring
 * Boot 4.0 yet.
 */
@Configuration
public class SolverConfiguration {

    @Bean
    public SolverManager<ScheduleSolution, Long> solverManager() {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(ScheduleSolution.class)
                .withEntityClasses(ScheduleAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSpentLimit(Duration.ofSeconds(30)));

        SolverFactory<ScheduleSolution> solverFactory = SolverFactory.create(solverConfig);
        return SolverManager.create(solverFactory);
    }
}
