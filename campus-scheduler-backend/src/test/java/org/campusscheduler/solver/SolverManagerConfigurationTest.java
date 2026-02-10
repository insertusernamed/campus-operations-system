package org.campusscheduler.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolverManagerConfigurationTest {

    private final SolverManagerConfiguration configuration = new SolverManagerConfiguration();

    @Test
    @DisplayName("should use 10s unimproved timeout by default")
    void shouldUseTenSecondUnimprovedTimeoutByDefault() {
        assertThat(configuration.calculateUnimprovedTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("should scale spent timeout by course count")
    void shouldScaleSpentTimeoutByCourseCount() {
        assertThat(configuration.calculateTimeout(200)).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.calculateTimeout(201)).isEqualTo(Duration.ofSeconds(60));
        assertThat(configuration.calculateTimeout(500)).isEqualTo(Duration.ofSeconds(60));
        assertThat(configuration.calculateTimeout(501)).isEqualTo(Duration.ofSeconds(120));
        assertThat(configuration.calculateTimeout(1000)).isEqualTo(Duration.ofSeconds(120));
        assertThat(configuration.calculateTimeout(1001)).isEqualTo(Duration.ofMinutes(3));
    }
}
