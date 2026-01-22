package org.campusscheduler.domain.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for analytics endpoints.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Utilization analytics and statistics")
@org.springframework.validation.annotation.Validated
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/rooms")
    @Operation(summary = "Get utilization for all rooms")
    public ResponseEntity<List<RoomUtilizationDTO>> getAllRoomsUtilization(
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return ResponseEntity.ok(analyticsService.getAllRoomsUtilization(semester));
    }

    @GetMapping("/rooms/{id}")
    @Operation(summary = "Get utilization for a specific room")
    public ResponseEntity<RoomUtilizationDTO> getRoomUtilization(
            @PathVariable Long id,
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return analyticsService.getRoomUtilization(id, semester)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buildings")
    @Operation(summary = "Get utilization for all buildings")
    public ResponseEntity<List<BuildingUtilizationDTO>> getAllBuildingsUtilization(
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return ResponseEntity.ok(analyticsService.getAllBuildingsUtilization(semester));
    }

    @GetMapping("/buildings/{id}")
    @Operation(summary = "Get utilization for a specific building")
    public ResponseEntity<BuildingUtilizationDTO> getBuildingUtilization(
            @PathVariable Long id,
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return analyticsService.getBuildingUtilization(id, semester)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Get peak hours sorted by booking count")
    public ResponseEntity<List<PeakHoursDTO>> getPeakHours(
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return ResponseEntity.ok(analyticsService.getPeakHours(semester));
    }

    @GetMapping("/underused")
    @Operation(summary = "Get underused rooms below utilization threshold")
    public ResponseEntity<List<RoomUtilizationDTO>> getUnderusedRooms(
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester,
            @Parameter(description = "Utilization threshold percentage (default: 30)") @RequestParam(defaultValue = "30.0") @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(100) double threshold) {
        return ResponseEntity.ok(analyticsService.getUnderusedRooms(semester, threshold));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get overall utilization summary")
    public ResponseEntity<UtilizationSummaryDTO> getUtilizationSummary(
            @Parameter(description = "Semester to filter by") @RequestParam @jakarta.validation.constraints.NotBlank String semester) {
        return ResponseEntity.ok(analyticsService.getUtilizationSummary(semester));
    }
}
