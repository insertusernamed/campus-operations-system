package org.campusscheduler.generator;

import java.util.Arrays;
import java.util.List;

import org.campusscheduler.generator.UniversityGeneratorService.GenerationConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationResult;
import org.campusscheduler.generator.UniversityGeneratorService.UniversityStats;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for generating demo data.
 * Provides endpoints to generate a complete university dataset for
 * presentations using research-backed ratios.
 */
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Data Generator", description = "Generate demo data for presentations")
public class DataGeneratorController {

	private final UniversityGeneratorService universityGeneratorService;

	/**
	 * Request DTO for generating university data using archetype and student population.
	 */
	public record GenerateRequest(
			String archetype,

			@Min(value = 1000, message = "Minimum 1000 students")
			@Max(value = 100000, message = "Maximum 100000 students")
			Integer studentPopulation) {

		/**
		 * Convert to GenerationConfig using research-based ratios.
		 */
		public GenerationConfig toConfig() {
			UniversityArchetype arch = archetype != null
					? UniversityArchetype.valueOf(archetype.toUpperCase())
					: UniversityArchetype.COMMUNITY;

			int population = studentPopulation != null
					? studentPopulation
					: 8000;

			return GenerationConfig.fromStudentPopulation(arch, population);
		}
	}

	/**
	 * DTO describing an archetype for the frontend.
	 * Note: studentsPerCourse is the S/C ratio (students per course offering).
	 * Higher values mean less course variety per student.
	 */
	public record ArchetypeInfo(
			String id,
			String displayName,
			String description,
			int studentsPerBuilding,
			int coursesPerBuilding,
			double studentsPerCourse,
			int minStudents,
			int maxStudents,
			double academicBuildingRatio,
			String[] exampleUniversities) {

		public static ArchetypeInfo from(UniversityArchetype archetype) {
			String[] examples = switch (archetype) {
				case METROPOLIS -> new String[]{"University of Toronto", "McGill University", "University of Waterloo"};
				case CAMPUS_SPRAWL -> new String[]{"University of British Columbia", "University of Alberta"};
				case COMMUNITY -> new String[]{"Lakehead University"};
			};

			return new ArchetypeInfo(
					archetype.name(),
					archetype.getDisplayName(),
					archetype.getDescription(),
					archetype.getStudentsPerBuilding(),
					archetype.getCoursesPerBuilding(),
					archetype.getStudentsPerCourse(),
					archetype.getMinStudents(),
					archetype.getMaxStudents(),
					archetype.getAcademicBuildingRatio(),
					examples
			);
		}
	}

	/**
	 * Get available archetypes with their research-based ratios.
	 */
	@GetMapping("/archetypes")
	@Operation(summary = "Get university archetypes", description = "Returns available archetypes with research-based ratios and descriptions")
	public ResponseEntity<List<ArchetypeInfo>> getArchetypes() {
		List<ArchetypeInfo> archetypes = Arrays.stream(UniversityArchetype.values())
				.map(ArchetypeInfo::from)
				.toList();
		return ResponseEntity.ok(archetypes);
	}

	/**
	 * Preview generation without actually creating data.
	 */
	@PostMapping("/preview")
	@Operation(summary = "Preview generation", description = "Returns what would be generated without creating data")
	public ResponseEntity<GenerationPreview> previewGeneration(
			@Valid @RequestBody(required = false) GenerateRequest request) {

		GenerationConfig config = request != null
				? request.toConfig()
				: GenerationConfig.defaultConfig();

			return ResponseEntity.ok(new GenerationPreview(
					config.archetype().name(),
					config.archetype().getDisplayName(),
					config.studentPopulation(),
					config.buildings(),
					config.academicBuildings(),
					config.roomsPerBuilding(),
					config.instructors(),
					config.courses(),
					config.academicBuildings() * config.roomsPerBuilding(),
					String.format("Using %s archetype: %d students/building, %d courses/building, %d instructional buildings",
							config.archetype().getDisplayName(),
							config.archetype().getStudentsPerBuilding(),
							config.archetype().getCoursesPerBuilding(),
							config.academicBuildings())
			));
		}

	/**
	 * Preview DTO showing what would be generated.
	 */
	public record GenerationPreview(
			String archetype,
			String archetypeDisplayName,
			int studentPopulation,
			int totalBuildings,
			int academicBuildings,
			int roomsPerBuilding,
			int instructors,
			int courses,
			int totalRooms,
			String ratioInfo) {
	}

	/**
	 * Generate a complete university dataset.
	 */
	@PostMapping("/university")
	@Operation(summary = "Generate complete university", description = "Creates buildings, rooms, instructors, and courses using research-based ratios. Clears existing data first.")
	public ResponseEntity<GenerationResult> generateUniversity(
			@Valid @RequestBody(required = false) GenerateRequest request) {

		GenerationConfig config = request != null
				? request.toConfig()
				: GenerationConfig.defaultConfig();

		log.info("Received generate request with config: {}", config);
		GenerationResult result = universityGeneratorService.generateUniversity(config);

		return ResponseEntity.ok(result);
	}

	/**
	 * Generate a small community university (5,000 students).
	 */
	@PostMapping("/university/small")
	@Operation(summary = "Generate small university", description = "Creates a small community university (5,000 students) based on Lakehead University ratios")
	public ResponseEntity<GenerationResult> generateSmallUniversity() {
		log.info("Generating small university");
		GenerationResult result = universityGeneratorService.generateUniversity(GenerationConfig.small());
		return ResponseEntity.ok(result);
	}

	/**
	 * Generate a large metropolis university (50,000 students).
	 */
	@PostMapping("/university/large")
	@Operation(summary = "Generate large university", description = "Creates a large urban university (50,000 students) based on U of T/McGill ratios")
	public ResponseEntity<GenerationResult> generateLargeUniversity() {
		log.info("Generating large university");
		GenerationResult result = universityGeneratorService.generateUniversity(GenerationConfig.large());
		return ResponseEntity.ok(result);
	}

	/**
	 * Generate a research campus university (40,000 students).
	 */
	@PostMapping("/university/research")
	@Operation(summary = "Generate research campus", description = "Creates a sprawling research university (40,000 students) based on UBC/Alberta ratios")
	public ResponseEntity<GenerationResult> generateResearchUniversity() {
		log.info("Generating research campus university");
		GenerationResult result = universityGeneratorService.generateUniversity(GenerationConfig.researchCampus());
		return ResponseEntity.ok(result);
	}

	/**
	 * Clear all data from the database.
	 *
	 * <p>
	 * <strong>WARNING:</strong> This endpoint deletes all data without
	 * confirmation.
	 * In production environments, ensure this endpoint is restricted to admin users
	 * only.
	 * See SecurityConfig for authentication/authorization configuration.
	 * </p>
	 */
	@DeleteMapping("/reset")
	@Operation(summary = "Reset database", description = "Clears all schedules, courses, instructors, rooms, and buildings. WARNING: For development/demo use only.")
	public ResponseEntity<Void> resetDatabase() {
		log.info("Resetting database");
		universityGeneratorService.clearAll();
		return ResponseEntity.noContent().build();
	}

	/**
	 * Get current database statistics.
	 */
	@GetMapping("/stats")
	@Operation(summary = "Get database stats", description = "Returns counts of buildings, rooms, instructors, courses, and schedules.")
	public ResponseEntity<UniversityStats> getStats() {
		return ResponseEntity.ok(universityGeneratorService.getStats());
	}
}
