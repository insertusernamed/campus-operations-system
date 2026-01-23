package org.campusscheduler.generator;

import org.campusscheduler.generator.UniversityGeneratorService.GenerationConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for generating demo data.
 * Provides endpoints to generate a complete university dataset for
 * presentations.
 */
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Generator", description = "Generate demo data for presentations")
public class DataGeneratorController {

    private final UniversityGeneratorService universityGeneratorService;

    /**
     * Request DTO for generating university data.
     */
    public record GenerateRequest(
            Integer buildings,
            Integer roomsPerBuilding,
            Integer instructors,
            Integer courses) {

        /**
         * Convert to GenerationConfig with defaults for null values.
         */
        public GenerationConfig toConfig() {
            GenerationConfig defaults = GenerationConfig.defaultConfig();
            return new GenerationConfig(
                    buildings != null ? buildings : defaults.buildings(),
                    roomsPerBuilding != null ? roomsPerBuilding : defaults.roomsPerBuilding(),
                    instructors != null ? instructors : defaults.instructors(),
                    courses != null ? courses : defaults.courses());
        }
    }

    /**
     * Generate a complete university dataset.
     */
    @PostMapping("/university")
    @Operation(summary = "Generate complete university", description = "Creates buildings, rooms, instructors, and courses. Clears existing data first.")
    public ResponseEntity<GenerationResult> generateUniversity(
            @RequestBody(required = false) GenerateRequest request) {

        GenerationConfig config = request != null
                ? request.toConfig()
                : GenerationConfig.defaultConfig();

        log.info("Received generate request with config: {}", config);
        GenerationResult result = universityGeneratorService.generateUniversity(config);

        return ResponseEntity.ok(result);
    }

    /**
     * Generate a small university for quick testing.
     */
    @PostMapping("/university/small")
    @Operation(summary = "Generate small university", description = "Creates a small dataset (4 buildings, 40 rooms, 50 instructors, 100 courses)")
    public ResponseEntity<GenerationResult> generateSmallUniversity() {
        log.info("Generating small university");
        GenerationResult result = universityGeneratorService.generateUniversity(GenerationConfig.small());
        return ResponseEntity.ok(result);
    }

    /**
     * Generate a large university for stress testing.
     */
    @PostMapping("/university/large")
    @Operation(summary = "Generate large university", description = "Creates a large dataset (12 buildings, 240 rooms, 300 instructors, 800 courses)")
    public ResponseEntity<GenerationResult> generateLargeUniversity() {
        log.info("Generating large university");
        GenerationResult result = universityGeneratorService.generateUniversity(GenerationConfig.large());
        return ResponseEntity.ok(result);
    }

    /**
     * Clear all data from the database.
     */
    @DeleteMapping("/reset")
    @Operation(summary = "Reset database", description = "Clears all schedules, courses, instructors, rooms, and buildings")
    public ResponseEntity<Void> resetDatabase() {
        log.info("Resetting database");
        universityGeneratorService.clearAll();
        return ResponseEntity.noContent().build();
    }
}
