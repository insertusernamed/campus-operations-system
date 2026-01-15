package org.campusscheduler.domain.building;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for Building entity using DataJpaTest.
 */
@DataJpaTest
class BuildingRepositoryTest {

	@Autowired
	private BuildingRepository buildingRepository;

	@Test
	@DisplayName("should find building by code")
	void shouldFindBuildingByCode() {
		Building building = Building.builder()
				.name("Science Building")
				.code("SCI")
				.address("123 Campus Drive")
				.build();
		buildingRepository.save(building);

		Optional<Building> result = buildingRepository.findByCode("SCI");

		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("Science Building");
	}

	@Test
	@DisplayName("should return empty when code not found")
	void shouldReturnEmptyWhenCodeNotFound() {
		Optional<Building> result = buildingRepository.findByCode("NONEXISTENT");

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("should check if code exists - true case")
	void shouldReturnTrueWhenCodeExists() {
		Building building = Building.builder()
				.name("Arts Building")
				.code("ART")
				.build();
		buildingRepository.save(building);

		assertThat(buildingRepository.existsByCode("ART")).isTrue();
	}

	@Test
	@DisplayName("should check if code exists - false case")
	void shouldReturnFalseWhenCodeDoesNotExist() {
		assertThat(buildingRepository.existsByCode("NONEXISTENT")).isFalse();
	}

	@Test
	@DisplayName("should save new building with generated id")
	void shouldSaveNewBuildingWithGeneratedId() {
		Building newBuilding = Building.builder()
				.name("Engineering Building")
				.code("ENG")
				.address("789 Campus Drive")
				.build();

		Building saved = buildingRepository.save(newBuilding);

		assertThat(saved.getId()).isNotNull();
		assertThat(buildingRepository.findById(saved.getId())).isPresent();
	}

	@Test
	@DisplayName("should delete building by id")
	void shouldDeleteBuildingById() {
		Building building = Building.builder()
				.name("To Delete")
				.code("DEL")
				.build();
		Building saved = buildingRepository.save(building);
		Long id = saved.getId();

		buildingRepository.deleteById(id);

		assertThat(buildingRepository.findById(id)).isEmpty();
	}
}
