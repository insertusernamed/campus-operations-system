package org.campusscheduler.domain.instructorpreference;

import org.campusscheduler.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstructorPreferenceController.class)
@Import(SecurityConfig.class)
class InstructorPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorPreferenceService preferenceService;

    @Test
    @DisplayName("GET returns default preferences payload")
    void getReturnsPreferences() throws Exception {
        InstructorPreferenceResponse response = new InstructorPreferenceResponse(
                10L,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                120,
                15,
                true,
                List.of(),
                List.of(),
                LocalDateTime.now());

        when(preferenceService.getByInstructorId(10L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/instructor-preferences/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorId").value(10))
                .andExpect(jsonPath("$.maxGapMinutes").value(120))
                .andExpect(jsonPath("$.minTravelBufferMinutes").value(15));
    }

    @Test
    @DisplayName("GET room-feature-options returns catalog payload")
    void getRoomFeatureOptionsReturnsOptions() throws Exception {
        when(preferenceService.getRoomFeatureOptions()).thenReturn(List.of(
                new RoomFeatureOptionResponse("projector", "Projector", "Presentation and AV"),
                new RoomFeatureOptionResponse("microphone", "Microphone", "Presentation and AV")));

        mockMvc.perform(get("/api/instructor-preferences/room-feature-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].value").value("projector"))
                .andExpect(jsonPath("$[1].value").value("microphone"));
    }

    @Test
    @DisplayName("PUT validates max gap range")
    void putValidatesPayload() throws Exception {
        mockMvc.perform(put("/api/instructor-preferences/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "preferredStartTime": "08:00",
                                  "preferredEndTime": "18:00",
                                  "maxGapMinutes": 400,
                                  "minTravelBufferMinutes": 15,
                                  "avoidBuildingHops": true,
                                  "preferredBuildingIds": [],
                                  "requiredRoomFeatures": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT returns 404 for unknown instructor")
    void putReturnsNotFoundWhenInstructorMissing() throws Exception {
        when(preferenceService.upsert(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/instructor-preferences/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "preferredStartTime": "09:00",
                                  "preferredEndTime": "17:00",
                                  "maxGapMinutes": 90,
                                  "minTravelBufferMinutes": 15,
                                  "avoidBuildingHops": true,
                                  "preferredBuildingIds": [1],
                                  "requiredRoomFeatures": ["projector"]
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}
