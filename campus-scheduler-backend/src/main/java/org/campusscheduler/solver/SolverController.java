package org.campusscheduler.solver;

import org.campusscheduler.solver.SolverService.SolverStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for the schedule solver.
 * Provides endpoints to start, stop, and monitor the optimization process.
 */
@RestController
@RequestMapping("/api/solver")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Solver", description = "Schedule optimization with Timefold Solver")
public class SolverController {

    private final SolverService solverService;

    /**
     * Response DTO for start solving.
     */
    public record StartResponse(Long problemId, String message) {
    }

    /**
     * Response DTO for save solution.
     */
    public record SaveResponse(int savedCount, String message) {
    }

    /**
     * Start the solver for a given semester.
     */
    @PostMapping("/start")
    @Operation(summary = "Start solving", description = "Begins the optimization process for scheduling courses")
    public ResponseEntity<StartResponse> startSolving(
            @RequestParam(defaultValue = "Fall 2026") String semester) {

        log.info("Starting solver for semester: {}", semester);
        Long problemId = solverService.startSolving(semester);

        return ResponseEntity.ok(new StartResponse(
                problemId,
                "Solver started for " + semester));
    }

    /**
     * Get the current solver status.
     */
    @GetMapping("/status")
    @Operation(summary = "Get solver status", description = "Returns current optimization status and score")
    public ResponseEntity<SolverStatusResponse> getStatus() {
        return ResponseEntity.ok(solverService.getStatus());
    }

    /**
     * Stop the currently running solver.
     */
    @PostMapping("/stop")
    @Operation(summary = "Stop solving", description = "Terminates the optimization process early")
    public ResponseEntity<String> stopSolving() {
        log.info("Stopping solver");
        solverService.stopSolving();
        return ResponseEntity.ok("Solver stopped");
    }

    /**
     * Save the current best solution to the database.
     */
    @PostMapping("/save")
    @Operation(summary = "Save solution", description = "Persists the current best solution as Schedule entities")
    public ResponseEntity<SaveResponse> saveSolution() {
        int count = solverService.saveSolution();
        return ResponseEntity.ok(new SaveResponse(
                count,
                "Saved " + count + " schedules"));
    }
}
