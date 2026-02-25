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
