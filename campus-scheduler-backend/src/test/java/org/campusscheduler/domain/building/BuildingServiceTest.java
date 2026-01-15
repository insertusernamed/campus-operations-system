package org.campusscheduler.domain.building;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BuildingService.
 */
@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @InjectMocks
    private BuildingService buildingService;

    private Building testBuilding;

    @BeforeEach
    void setUp() {
        testBuilding = Building.builder()
                .id(1L)
                .name("Science Building")
                .code("SCI")
                .address("123 Campus Drive")
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all buildings")
        void shouldReturnAllBuildings() {
            Building building2 = Building.builder()
                    .id(2L)
                    .name("Arts Building")
                    .code("ART")
                    .build();

            when(buildingRepository.findAll()).thenReturn(List.of(testBuilding, building2));

            List<Building> result = buildingService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Building::getCode).containsExactly("SCI", "ART");
            verify(buildingRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no buildings exist")
        void shouldReturnEmptyListWhenNoBuildingsExist() {
            when(buildingRepository.findAll()).thenReturn(List.of());

            List<Building> result = buildingService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return building when found")
        void shouldReturnBuildingWhenFound() {
            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));

            Optional<Building> result = buildingService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("SCI");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Building> result = buildingService.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("should return building when code exists")
        void shouldReturnBuildingWhenCodeExists() {
            when(buildingRepository.findByCode("SCI")).thenReturn(Optional.of(testBuilding));

            Optional<Building> result = buildingService.findByCode("SCI");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Science Building");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create building when code is unique")
        void shouldCreateBuildingWhenCodeIsUnique() {
            Building newBuilding = Building.builder()
                    .name("New Building")
                    .code("NEW")
                    .build();

            when(buildingRepository.existsByCode("NEW")).thenReturn(false);
            when(buildingRepository.save(newBuilding)).thenReturn(
                    Building.builder().id(2L).name("New Building").code("NEW").build());

            Building result = buildingService.create(newBuilding);

            assertThat(result.getId()).isEqualTo(2L);
            verify(buildingRepository).save(newBuilding);
        }

        @Test
        @DisplayName("should throw exception when code already exists")
        void shouldThrowExceptionWhenCodeAlreadyExists() {
            Building duplicateBuilding = Building.builder()
                    .name("Duplicate Building")
                    .code("SCI")
                    .build();

            when(buildingRepository.existsByCode("SCI")).thenReturn(true);

            assertThatThrownBy(() -> buildingService.create(duplicateBuilding))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Building code already exists: SCI");

            verify(buildingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update building when found")
        void shouldUpdateBuildingWhenFound() {
            Building updated = Building.builder()
                    .name("Updated Name")
                    .code("SCI")
                    .address("New Address")
                    .build();

            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
            when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArgument(0));

            Optional<Building> result = buildingService.update(1L, updated);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Updated Name");
            assertThat(result.get().getAddress()).isEqualTo("New Address");
        }

        @Test
        @DisplayName("should return empty when building not found")
        void shouldReturnEmptyWhenBuildingNotFound() {
            when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Building> result = buildingService.update(999L, testBuilding);

            assertThat(result).isEmpty();
            verify(buildingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when changing to existing code")
        void shouldThrowExceptionWhenChangingToExistingCode() {
            Building existingOther = Building.builder()
                    .id(2L)
                    .code("ART")
                    .build();

            Building updated = Building.builder()
                    .name("Updated")
                    .code("ART")
                    .build();

            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
            when(buildingRepository.findByCode("ART")).thenReturn(Optional.of(existingOther));

            assertThatThrownBy(() -> buildingService.update(1L, updated))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Building code already in use: ART");

            verify(buildingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should return true when building is deleted")
        void shouldReturnTrueWhenBuildingIsDeleted() {
            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));

            boolean result = buildingService.delete(1L);

            assertThat(result).isTrue();
            verify(buildingRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should return false when building not found")
        void shouldReturnFalseWhenBuildingNotFound() {
            when(buildingRepository.findById(999L)).thenReturn(Optional.empty());

            boolean result = buildingService.delete(999L);

            assertThat(result).isFalse();
            verify(buildingRepository, never()).deleteById(any());
        }
    }
}
