package org.campusscheduler.solver;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing the Timefold solver.
 * Handles starting, stopping, and retrieving solutions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolverService {

    private final SolverManager<ScheduleSolution, Long> solverManager;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ScheduleRepository scheduleRepository;

    private static final AtomicLong problemIdGenerator = new AtomicLong(0);
    private final AtomicReference<Long> currentProblemId = new AtomicReference<>();
    private final AtomicReference<ScheduleSolution> bestSolution = new AtomicReference<>();

    /**
     * DTO for solver status.
     */
    public record SolverStatusResponse(
            SolverStatus status,
            HardSoftScore score,
            int assignedCourses,
            int totalCourses,
            long hardViolations,
            long softScore) {
    }

    /**
     * Start solving for the given semester.
     * Terminates any previously running solver before starting.
     *
     * @param semester the semester to schedule
     * @return the problem ID
     */
    @Transactional(readOnly = true)
    public Long startSolving(String semester) {
        // Terminate any previous solver job
        Long previousId = currentProblemId.get();
        if (previousId != null && solverManager.getSolverStatus(previousId) == SolverStatus.SOLVING_ACTIVE) {
            log.info("Terminating previous solver job {}", previousId);
            solverManager.terminateEarly(previousId);
        }

        Long problemId = problemIdGenerator.incrementAndGet();
        currentProblemId.set(problemId);

        log.info("Starting solver for semester {} with problem ID {}", semester, problemId);

        ScheduleSolution problem = buildProblem(semester);
        bestSolution.set(problem);

        solverManager.solveBuilder()
                .withProblemId(problemId)
                .withProblem(problem)
                .withBestSolutionEventConsumer(event -> {
                    log.debug("New best solution: {}", event.solution().getScore());
                    bestSolution.set(event.solution());
                })
                .withExceptionHandler((id, exception) -> log.error("Solver failed for problem {}", id, exception))
                .run();

        return problemId;
    }

    /**
     * Get the current solver status.
     */
    public SolverStatusResponse getStatus() {
        Long problemId = currentProblemId.get();
        if (problemId == null) {
            return new SolverStatusResponse(
                    SolverStatus.NOT_SOLVING, null, 0, 0, 0, 0);
        }

        SolverStatus status = solverManager.getSolverStatus(problemId);
        ScheduleSolution solution = bestSolution.get();

        if (solution == null) {
            return new SolverStatusResponse(status, null, 0, 0, 0, 0);
        }

        long assignedCourses = solution.getAssignments().stream()
                .filter(ScheduleAssignment::isInitialized)
                .count();

        HardSoftScore score = solution.getScore();
        long hardViolations = score != null ? -score.hardScore() : 0;
        long softScore = score != null ? score.softScore() : 0;

        return new SolverStatusResponse(
                status,
                score,
                (int) assignedCourses,
                solution.getAssignments().size(),
                hardViolations,
                softScore);
    }

    /**
     * Stop the current solving process.
     */
    public void stopSolving() {
        Long problemId = currentProblemId.get();
        if (problemId != null) {
            log.info("Stopping solver for problem ID {}", problemId);
            solverManager.terminateEarly(problemId);
        }
    }

    /**
     * Get the current best solution.
     */
    public ScheduleSolution getBestSolution() {
        return bestSolution.get();
    }

    /**
     * Save the current solution as Schedule entities.
     */
    @Transactional
    public int saveSolution() {
        ScheduleSolution solution = bestSolution.get();
        if (solution == null || solution.getAssignments() == null) {
            return 0;
        }

        // Clear existing schedules for this semester
        String semester = solution.getSemester();
        scheduleRepository.deleteBySemester(semester);

        int count = 0;
        for (ScheduleAssignment assignment : solution.getAssignments()) {
            if (assignment.isInitialized()) {
                Schedule schedule = Schedule.builder()
                        .course(assignment.getCourse())
                        .room(assignment.getRoom())
                        .timeSlot(assignment.getTimeSlot())
                        .semester(semester)
                        .build();
                scheduleRepository.save(schedule);
                count++;
            }
        }

        log.info("Saved {} schedules for semester {}", count, semester);
        return count;
    }

    /**
     * Build the initial problem from database data.
     * Note: We fetch all data within a transaction to avoid lazy loading issues.
     * Course.instructor is eagerly initialized here.
     */
    private ScheduleSolution buildProblem(String semester) {
        List<Course> courses = courseRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<TimeSlot> timeSlots = timeSlotRepository.findAll();

        // Force initialization of lazy associations within transaction
        courses.forEach(course -> {
            if (course.getInstructor() != null) {
                course.getInstructor().getId(); // Initialize proxy
            }
        });

        log.info("Building problem: {} courses, {} rooms, {} time slots",
                courses.size(), rooms.size(), timeSlots.size());

        List<ScheduleAssignment> assignments = courses.stream()
                .map(course -> {
                    ScheduleAssignment assignment = new ScheduleAssignment();
                    assignment.setId(course.getId());
                    assignment.setCourse(course);
                    assignment.setSemester(semester);
                    // Room and TimeSlot are null - Timefold will assign them
                    return assignment;
                })
                .collect(Collectors.toList());

        return ScheduleSolution.builder()
                .rooms(rooms)
                .timeSlots(timeSlots)
                .assignments(assignments)
                .semester(semester)
                .build();
    }
}
