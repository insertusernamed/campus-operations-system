package org.campusscheduler.solver;

import org.campusscheduler.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImpactAnalysisController.class)
@Import(SecurityConfig.class)
class ImpactAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImpactAnalysisService impactAnalysisService;

    @Nested
    @DisplayName("POST /api/solver/impact")
    class Analyze {

        @Test
        @DisplayName("should return solved impact analysis")
        void shouldReturnSolvedImpactAnalysis() throws Exception {
            ImpactAnalysisResponse response = ImpactAnalysisResponse.builder()
                    .status(ImpactAnalysisResponse.Status.SOLVED)
                    .score("0hard/-5soft")
                    .scoreSummary("All hard constraints satisfied")
                    .moves(List.of())
                    .constraintSummaries(List.of())
                    .build();

            when(impactAnalysisService.analyze(any(ImpactAnalysisRequest.class)))
                    .thenReturn(Optional.of(response));

            mockMvc.perform(post("/api/solver/impact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "scheduleId": 1,
                              "proposedRoomId": 2
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SOLVED")))
                    .andExpect(jsonPath("$.score", is("0hard/-5soft")));
        }

        @Test
        @DisplayName("should return 404 when schedule is not found")
        void shouldReturn404WhenScheduleIsNotFound() throws Exception {
            when(impactAnalysisService.analyze(any(ImpactAnalysisRequest.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/solver/impact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "scheduleId": 999,
                              "proposedRoomId": 2
                            }
                            """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 for invalid impact analysis request")
        void shouldReturn409ForInvalidImpactAnalysisRequest() throws Exception {
            when(impactAnalysisService.analyze(any(ImpactAnalysisRequest.class)))
                    .thenThrow(new ImpactAnalysisStateException("At least one proposed change is required"));

            mockMvc.perform(post("/api/solver/impact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "scheduleId": 1
                            }
                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", is("At least one proposed change is required")))
                    .andExpect(jsonPath("$.code", is("IMPACT_STATE")));
        }
    }
}
