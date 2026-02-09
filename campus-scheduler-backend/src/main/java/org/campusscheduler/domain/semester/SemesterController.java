package org.campusscheduler.domain.semester;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for semester metadata.
 */
@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
@Tag(name = "Semesters", description = "Semester definitions and metadata")
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    @Operation(summary = "Get semester definitions")
    public ResponseEntity<List<SemesterDefinitionDTO>> getDefinitions() {
        return ResponseEntity.ok(semesterService.getDefinitions());
    }
}

