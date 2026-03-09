package org.campusscheduler.domain.instructorinsight;

import org.campusscheduler.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstructorInsightsController.class)
@Import(SecurityConfig.class)
class InstructorInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorInsightsService insightsService;

    @Test
    @DisplayName("GET summary returns snapshot payload")
    void getSummaryReturnsPayload() throws Exception {
        InstructorInsightsSummaryResponse summary = new InstructorInsightsSummaryResponse(
                8,
                2,
                1,
                3,
                2,
                1);

        when(insightsService.getSummary("Fall 2026")).thenReturn(summary);

        mockMvc.perform(get("/api/instructor-insights/summary")
                        .param("semester", "Fall 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstructors").value(8))
                .andExpect(jsonPath("$.overloadRisk").value(1));
    }

    @Test
    @DisplayName("GET queue returns prioritized rows")
    void getQueueReturnsPayload() throws Exception {
        InstructorQueueRowResponse row = new InstructorQueueRowResponse(
                10L,
                "Ada",
                "Lovelace",
                "Ada Lovelace",
                "ada@campus.edu",
                "Computer Science",
                3,
                9,
                6,
                12,
                InstructorLoadStatus.BALANCED,
                80,
                5,
                2,
                InstructorFrictionSeverity.MEDIUM,
                InstructorCoverageRiskLevel.MEDIUM,
                InstructorOperationalStatus.COVERAGE_RISK,
                0,
                0,
                List.of("Assign to open course(s)"));

        when(insightsService.getQueue("Fall 2026", "coverage-risk", "Computer Science"))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/instructor-insights/queue")
                        .param("semester", "Fall 2026")
                        .param("filter", "coverage-risk")
                        .param("department", "Computer Science"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("COVERAGE_RISK"));
    }

    @Test
    @DisplayName("GET load distribution returns department metrics")
    void getLoadDistributionReturnsPayload() throws Exception {
        InstructorLoadDistributionResponse response = new InstructorLoadDistributionResponse(
                "Fall 2026",
                List.of(new InstructorDepartmentLoadResponse(
                        "Computer Science",
                        4,
                        33,
                        24,
                        48,
                        2,
                        6,
                        InstructorCoverageRiskLevel.MEDIUM)));

        when(insightsService.getLoadDistribution("Fall 2026")).thenReturn(response);

        mockMvc.perform(get("/api/instructor-insights/load-distribution")
                        .param("semester", "Fall 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semester").value("Fall 2026"))
                .andExpect(jsonPath("$.departments[0].department").value("Computer Science"));
    }

    @Test
    @DisplayName("GET workbench returns payload")
    void getWorkbenchReturnsPayload() throws Exception {
        InstructorWorkbenchResponse workbench = new InstructorWorkbenchResponse(
                10L,
                "Ada",
                "Lovelace",
                "ada@campus.edu",
                "Computer Science",
                "SCI-201",
                "Fall 2026",
                3,
                9,
                6,
                12,
                InstructorLoadStatus.BALANCED,
                80,
                5,
                new InstructorFrictionSummaryResponse(2, 0, 1, 1),
                new InstructorLoadTrendResponse(6, 8.0, -2.0, "DOWN"),
                List.of(new InstructorWeeklyDensityResponse("Mon", 2, 150)),
                List.of(),
                List.of(),
                List.of(),
                List.of("Review friction issues"));

        when(insightsService.getWorkbench(10L, "Fall 2026")).thenReturn(Optional.of(workbench));

        mockMvc.perform(get("/api/instructor-insights/10/workbench")
                        .param("semester", "Fall 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorId").value(10))
                .andExpect(jsonPath("$.loadStatus").value("BALANCED"));
    }

    @Test
    @DisplayName("GET workbench returns 404 for unknown instructor")
    void getWorkbenchReturnsNotFound() throws Exception {
        when(insightsService.getWorkbench(999L, "Fall 2026")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/instructor-insights/999/workbench")
                        .param("semester", "Fall 2026"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET frictions returns insight payload")
    void getFrictionsReturnsPayload() throws Exception {
        InstructorFrictionIssueResponse issue = new InstructorFrictionIssueResponse(
                "friction-1",
                InstructorFrictionType.LARGE_GAP,
                InstructorFrictionSeverity.MEDIUM,
                42L,
                "Gap exceeds preference",
                RecommendedIssue.GAP_TOO_LARGE_BEFORE);

        when(insightsService.findFrictions(10L, "Fall 2026"))
                .thenReturn(Optional.of(List.of(issue)));

        mockMvc.perform(get("/api/instructor-insights/frictions")
                        .param("instructorId", "10")
                        .param("semester", "Fall 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("friction-1"))
                .andExpect(jsonPath("$[0].type").value("LARGE_GAP"));
    }

    @Test
    @DisplayName("GET frictions returns 404 for unknown instructor")
    void getFrictionsReturnsNotFound() throws Exception {
        when(insightsService.findFrictions(999L, "Fall 2026"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/instructor-insights/frictions")
                        .param("instructorId", "999")
                        .param("semester", "Fall 2026"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET frictions validates required semester")
    void getFrictionsValidatesSemester() throws Exception {
        mockMvc.perform(get("/api/instructor-insights/frictions")
                        .param("instructorId", "10")
                        .param("semester", ""))
                .andExpect(status().isBadRequest());
    }
}
