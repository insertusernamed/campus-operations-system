package org.campusscheduler.domain.instructorpreference;

import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorPreferenceServiceTest {

    @Mock
    private InstructorPreferenceRepository preferenceRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @InjectMocks
    private InstructorPreferenceService preferenceService;

    @Test
    void getByInstructorIdReturnsDefaultsWhenNoSavedPreference() {
        Instructor instructor = Instructor.builder().id(7L).build();
        when(instructorRepository.findById(7L)).thenReturn(Optional.of(instructor));
        when(preferenceRepository.findByInstructorId(7L)).thenReturn(Optional.empty());

        Optional<InstructorPreferenceResponse> response = preferenceService.getByInstructorId(7L);

        assertThat(response).isPresent();
        assertThat(response.get().preferredStartTime()).isEqualTo(InstructorPreferenceService.DEFAULT_PREFERRED_START);
        assertThat(response.get().preferredEndTime()).isEqualTo(InstructorPreferenceService.DEFAULT_PREFERRED_END);
        assertThat(response.get().maxGapMinutes()).isEqualTo(InstructorPreferenceService.DEFAULT_MAX_GAP_MINUTES);
        assertThat(response.get().minTravelBufferMinutes()).isEqualTo(InstructorPreferenceService.DEFAULT_MIN_TRAVEL_BUFFER_MINUTES);
        assertThat(response.get().avoidBuildingHops()).isTrue();
        assertThat(response.get().preferredBuildingIds()).isEmpty();
        assertThat(response.get().requiredRoomFeatures()).isEmpty();
    }

    @Test
    void upsertRejectsInvalidTimeRange() {
        InstructorPreferenceUpdateRequest request = new InstructorPreferenceUpdateRequest();
        request.setPreferredStartTime(LocalTime.of(15, 0));
        request.setPreferredEndTime(LocalTime.of(9, 0));
        request.setMaxGapMinutes(120);
        request.setMinTravelBufferMinutes(15);
        request.setAvoidBuildingHops(true);

        assertThatThrownBy(() -> preferenceService.upsert(9L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preferred start time");
    }

    @Test
    void upsertSanitizesAndPersistsNormalizedValues() {
        Instructor instructor = Instructor.builder().id(11L).build();
        when(instructorRepository.findById(11L)).thenReturn(Optional.of(instructor));
        when(preferenceRepository.findByInstructorId(11L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(InstructorPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorPreferenceUpdateRequest request = new InstructorPreferenceUpdateRequest();
        request.setPreferredStartTime(LocalTime.of(9, 0));
        request.setPreferredEndTime(LocalTime.of(17, 0));
        request.setMaxGapMinutes(90);
        request.setMinTravelBufferMinutes(20);
        request.setAvoidBuildingHops(true);
        request.setPreferredBuildingIds(Arrays.asList(3L, 2L, 3L, null, -1L));
        request.setRequiredRoomFeatures(Arrays.asList(" Projector ", "", null, "projector", "Mic"));

        Optional<InstructorPreferenceResponse> response = preferenceService.upsert(11L, request);

        assertThat(response).isPresent();
        assertThat(response.get().preferredBuildingIds()).containsExactly(2L, 3L);
        assertThat(response.get().requiredRoomFeatures()).containsExactly("projector", "mic");
        assertThat(response.get().maxGapMinutes()).isEqualTo(90);
        assertThat(response.get().minTravelBufferMinutes()).isEqualTo(20);
        assertThat(response.get().updatedAt()).isNotNull();
    }
}
