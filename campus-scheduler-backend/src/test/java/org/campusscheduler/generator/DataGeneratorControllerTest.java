package org.campusscheduler.generator;

import org.campusscheduler.config.SecurityConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UniversityGeneratorService universityGeneratorService;

    @Test
    @DisplayName("POST /api/generator/university should generate with default config")
    void shouldGenerateWithDefaultConfig() throws Exception {
        GenerationResult result = new GenerationResult(8, 120, 200, 500, 30);
        when(universityGeneratorService.generateUniversity(any(GenerationConfig.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/generator/university")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(8))
                .andExpect(jsonPath("$.rooms").value(120))
                .andExpect(jsonPath("$.instructors").value(200))
                .andExpect(jsonPath("$.courses").value(500));
    }

    @Test
    @DisplayName("POST /api/generator/university should accept custom config")
    void shouldAcceptCustomConfig() throws Exception {
        GenerationResult result = new GenerationResult(4, 40, 50, 100, 30);
        when(universityGeneratorService.generateUniversity(any(GenerationConfig.class)))
                .thenReturn(result);

        String requestBody = """
                {
                    "buildings": 4,
                    "roomsPerBuilding": 10,
                    "instructors": 50,
                    "courses": 100
                }
                """;

        mockMvc.perform(post("/api/generator/university")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(4));

        verify(universityGeneratorService).generateUniversity(any(GenerationConfig.class));
    }

    @Test
    @DisplayName("POST /api/generator/university/small should use small config")
    void shouldGenerateSmallUniversity() throws Exception {
        GenerationResult result = new GenerationResult(4, 40, 50, 100, 30);
        when(universityGeneratorService.generateUniversity(GenerationConfig.small()))
                .thenReturn(result);

        mockMvc.perform(post("/api/generator/university/small"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(4));
    }

    @Test
    @DisplayName("POST /api/generator/university/large should use large config")
    void shouldGenerateLargeUniversity() throws Exception {
        GenerationResult result = new GenerationResult(12, 240, 300, 800, 30);
        when(universityGeneratorService.generateUniversity(GenerationConfig.large()))
                .thenReturn(result);

        mockMvc.perform(post("/api/generator/university/large"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildings").value(12));
    }

    @Test
    @DisplayName("DELETE /api/generator/reset should clear database")
    void shouldResetDatabase() throws Exception {
        mockMvc.perform(delete("/api/generator/reset"))
                .andExpect(status().isNoContent());

        verify(universityGeneratorService).clearAll();
    }
}
