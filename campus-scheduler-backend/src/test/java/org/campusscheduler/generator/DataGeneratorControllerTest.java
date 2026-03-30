package org.campusscheduler.generator;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationResult;
import org.campusscheduler.generator.UniversityGeneratorService.UniversityStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for DataGeneratorController.
 */
@WebMvcTest(DataGeneratorController.class)
@Import(SecurityConfig.class)
class DataGeneratorControllerTest {

    private static final String AUTH_HEADER = "Basic YWRtaW46YWRtaW4=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UniversityGeneratorService universityGeneratorService;

    @Test
    @DisplayName("POST /api/generator/university should generate with default config")
    void shouldGenerateWithDefaultConfig() throws Exception {
        GenerationResult result = new GenerationResult("COMMUNITY", 8000, 8, 120, 200, 500, 8000, 48000, 30, "Test ratios");
        when(universityGeneratorService.generateUniversity(any(GenerationConfig.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/generator/university")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(8))
                .andExpect(jsonPath("$.rooms").value(120))
                .andExpect(jsonPath("$.instructors").value(200))
                .andExpect(jsonPath("$.courses").value(500))
                .andExpect(jsonPath("$.students").value(8000))
                .andExpect(jsonPath("$.generatedDemandCount").value(48000));
    }

    @Test
    @DisplayName("POST /api/generator/university should accept custom config")
    void shouldAcceptCustomConfig() throws Exception {
        GenerationResult result = new GenerationResult("COMMUNITY", 8000, 4, 40, 50, 100, 8000, 40000, 30, "Test ratios");
        when(universityGeneratorService.generateUniversity(any(GenerationConfig.class)))
                .thenReturn(result);

        String requestBody = """
                {
                    "studentPopulation": 6000,
                    "buildings": 4,
                    "roomsPerBuilding": 10,
                    "instructors": 50,
                    "courses": 100
                }
                """;

        mockMvc.perform(post("/api/generator/university")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(4));

        ArgumentCaptor<GenerationConfig> configCaptor = ArgumentCaptor.forClass(GenerationConfig.class);
        verify(universityGeneratorService, times(1)).generateUniversity(configCaptor.capture());
        GenerationConfig config = configCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(config.studentPopulation()).isEqualTo(6000);
        org.assertj.core.api.Assertions.assertThat(config.buildings()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(config.academicBuildings()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(config.roomsPerBuilding()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(config.instructors()).isEqualTo(50);
        org.assertj.core.api.Assertions.assertThat(config.courses()).isEqualTo(100);
    }

    @Test
    @DisplayName("POST /api/generator/university/small should use small config")
    void shouldGenerateSmallUniversity() throws Exception {
        GenerationResult result = new GenerationResult("COMMUNITY", 5000, 4, 40, 50, 100, 5000, 25000, 30, "Small config");
        when(universityGeneratorService.generateUniversity(GenerationConfig.small()))
                .thenReturn(result);

        mockMvc.perform(post("/api/generator/university/small")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(4));
    }

    @Test
    @DisplayName("POST /api/generator/university/large should use large config")
    void shouldGenerateLargeUniversity() throws Exception {
        GenerationResult result = new GenerationResult("METROPOLIS", 50000, 12, 240, 300, 800, 50000, 250000, 30, "Large config");
        when(universityGeneratorService.generateUniversity(GenerationConfig.large()))
                .thenReturn(result);

        mockMvc.perform(post("/api/generator/university/large")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(12));
    }

    @Test
    @DisplayName("DELETE /api/generator/reset should clear database")
    void shouldResetDatabase() throws Exception {
        mockMvc.perform(delete("/api/generator/reset")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());

        verify(universityGeneratorService).clearAll();
    }

    @Test
    @DisplayName("GET /api/generator/stats should include student demand metrics")
    void shouldReturnStats() throws Exception {
        UniversityStats stats = new UniversityStats(4, 40, 50, 100, 30, 5000, 25000);
        when(universityGeneratorService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/generator/stats")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(5000))
                .andExpect(jsonPath("$.generatedDemandCount").value(25000));
    }
}
