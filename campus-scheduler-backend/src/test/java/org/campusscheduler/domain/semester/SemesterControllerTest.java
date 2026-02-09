package org.campusscheduler.domain.semester;

import org.campusscheduler.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for SemesterController.
 */
@WebMvcTest(SemesterController.class)
@Import(SecurityConfig.class)
class SemesterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SemesterService semesterService;

    @Test
    @DisplayName("should return all semester definitions")
    void shouldReturnAllSemesterDefinitions() throws Exception {
        when(semesterService.getDefinitions()).thenReturn(List.of(
                new SemesterDefinitionDTO(SemesterTerm.WINTER, "Winter", 12, 21, 3, 20, -1, 0),
                new SemesterDefinitionDTO(SemesterTerm.SPRING, "Spring", 3, 21, 6, 20, 0, 0),
                new SemesterDefinitionDTO(SemesterTerm.SUMMER, "Summer", 6, 21, 9, 20, 0, 0),
                new SemesterDefinitionDTO(SemesterTerm.FALL, "Fall", 9, 21, 12, 20, 0, 0)
        ));

        mockMvc.perform(get("/api/semesters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].term").value("WINTER"))
                .andExpect(jsonPath("$[0].displayName").value("Winter"))
                .andExpect(jsonPath("$[0].startMonth").value(12))
                .andExpect(jsonPath("$[0].endMonth").value(3))
                .andExpect(jsonPath("$[0].startYearOffset").value(-1))
                .andExpect(jsonPath("$[3].term").value("FALL"))
                .andExpect(jsonPath("$[3].displayName").value("Fall"));
    }
}
