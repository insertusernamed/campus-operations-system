package org.campusscheduler.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import org.campusscheduler.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolverController.class)
@Import(SecurityConfig.class)
class SolverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolverService solverService;

    @Nested
    @DisplayName("POST /api/solver/start")
    class StartSolving {

        @Test
        @DisplayName("should return started response with problem ID")
        void shouldReturnStartedResponseWithProblemId() throws Exception {
            when(solverService.startSolving("Fall 2027")).thenReturn(77L);

            mockMvc.perform(post("/api/solver/start")
                    .param("semester", "Fall 2027"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.problemId", is(77)))
                    .andExpect(jsonPath("$.message", is("Solver started for Fall 2027")));
        }
    }

    @Nested
    @DisplayName("GET /api/solver/status")
    class GetStatus {

        @Test
        @DisplayName("should return solver status payload")
        void shouldReturnSolverStatusPayload() throws Exception {
            SolverService.SolverStatusResponse response = new SolverService.SolverStatusResponse(
                    SolverStatus.NOT_SOLVING,
                    HardSoftScore.of(0, -10),
                    10,
                    20,
                    0,
                    -10);

            when(solverService.getStatus()).thenReturn(response);

            mockMvc.perform(get("/api/solver/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("NOT_SOLVING")))
                    .andExpect(jsonPath("$.assignedCourses", is(10)))
                    .andExpect(jsonPath("$.totalCourses", is(20)))
                    .andExpect(jsonPath("$.hardViolations", is(0)))
                    .andExpect(jsonPath("$.softScore", is(-10)));
        }
    }

    @Nested
    @DisplayName("GET /api/solver/analytics")
    class GetAnalytics {

        @Test
        @DisplayName("should return analytics for requested semester")
        void shouldReturnAnalyticsForRequestedSemester() throws Exception {
            SolverService.SolverAnalyticsResponse analyticsResponse = new SolverService.SolverAnalyticsResponse(
                    "Fall 2026",
                    20,
                    5,
                    40,
                    100,
                    40.0,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());

            when(solverService.getAnalytics("Fall 2026")).thenReturn(analyticsResponse);

            mockMvc.perform(get("/api/solver/analytics")
                    .param("semester", "Fall 2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.semester", is("Fall 2026")))
                    .andExpect(jsonPath("$.totalRooms", is(20)))
                    .andExpect(jsonPath("$.overallUtilizationPercentage", is(40.0)));
        }
    }

    @Nested
    @DisplayName("POST /api/solver/save")
    class SaveSolution {

        @Test
        @DisplayName("should return saved schedule count")
        void shouldReturnSavedScheduleCount() throws Exception {
            when(solverService.saveSolution()).thenReturn(12);

            mockMvc.perform(post("/api/solver/save"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.savedCount", is(12)))
                    .andExpect(jsonPath("$.message", is("Saved 12 schedules")));
        }
    }
}
