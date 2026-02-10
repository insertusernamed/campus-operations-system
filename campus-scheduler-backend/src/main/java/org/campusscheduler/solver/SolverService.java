package org.campusscheduler.solver;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.campusscheduler.websocket.SolverProgressEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PreDestroy;

/**
 * Service for managing the Timefold solver.
 * Uses Solver directly (instead of SolverManager) to enable PhaseLifecycleListeners
 * for real-time progress updates during Construction Heuristic.
 */
@Service
@Slf4j
public class SolverService {

	private static final String SOLVER_TOPIC = "/topic/solver/progress";
	private static final long BROADCAST_THROTTLE_MS = 200; // Min time between broadcasts

	private final SolverManagerConfiguration solverConfig;
	private final CourseRepository courseRepository;
	private final RoomRepository roomRepository;
	private final TimeSlotRepository timeSlotRepository;
	private final ScheduleRepository scheduleRepository;
	private final SimpMessagingTemplate messagingTemplate;

	private final ExecutorService solverExecutor = Executors.newSingleThreadExecutor();
	private final AtomicReference<Solver<ScheduleSolution>> currentSolver = new AtomicReference<>();
	private final AtomicReference<ScheduleSolution> bestSolution = new AtomicReference<>();
	private final AtomicReference<SolverStatus> solverStatus = new AtomicReference<>(SolverStatus.NOT_SOLVING);

	private volatile long solverStartTime;
	private volatile long lastBroadcastTime;

	public SolverService(
			SolverManagerConfiguration solverConfig,
			CourseRepository courseRepository,
			RoomRepository roomRepository,
			TimeSlotRepository timeSlotRepository,
			ScheduleRepository scheduleRepository,
			SimpMessagingTemplate messagingTemplate) {
		this.solverConfig = solverConfig;
		this.courseRepository = courseRepository;
		this.roomRepository = roomRepository;
		this.timeSlotRepository = timeSlotRepository;
		this.scheduleRepository = scheduleRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@PreDestroy
	public void shutdown() {
		stopSolving();
		solverExecutor.shutdownNow();
	}

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

	public record SolverRoomUtilization(
			Long roomId,
			String roomNumber,
			String buildingName,
			String buildingCode,
			Integer capacity,
			long scheduledSlots,
			long totalSlots,
			double utilizationPercentage) {
	}

	public record SolverBuildingUtilization(
			Long buildingId,
			String buildingName,
			String buildingCode,
			int roomCount,
			long scheduledSlots,
			long totalSlots,
			double utilizationPercentage) {
	}

	public record SolverPeakHours(
			Long timeSlotId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime,
			String label,
			long bookingCount) {
	}

	public record SolverAnalyticsResponse(
			String semester,
			int totalRooms,
			int totalBuildings,
			long totalScheduledSlots,
			long totalAvailableSlots,
			double overallUtilizationPercentage,
			List<SolverRoomUtilization> topUtilizedRooms,
			List<SolverRoomUtilization> leastUtilizedRooms,
			List<SolverRoomUtilization> rooms,
			List<SolverBuildingUtilization> buildings,
			List<SolverPeakHours> peakHours) {
	}

	/**
	 * Start solving for the given semester.
	 * Terminates any previously running solver before starting.
	 */
	@Transactional(readOnly = true)
	public Long startSolving(String semester) {
		// Terminate any previous solver
		stopSolving();

		log.info("Starting solver for semester {}", semester);

		ScheduleSolution problem = buildProblem(semester);
		bestSolution.set(problem);
		solverStartTime = System.currentTimeMillis();
		lastBroadcastTime = 0;

		// Create solver with timeout based on problem size
		int courseCount = problem.getAssignments().size();
		Duration spentLimit = solverConfig.calculateTimeout(courseCount);
		Duration unimprovedLimit = solverConfig.calculateUnimprovedTimeout();
		SolverFactory<ScheduleSolution> factory = solverConfig.createSolverFactory(courseCount);
		Solver<ScheduleSolution> solver = factory.buildSolver();
		log.info("Solver limits set: spent={}, unimproved={} for {} courses",
				spentLimit, unimprovedLimit, courseCount);
		currentSolver.set(solver);

		// Add listener for step-by-step progress updates
		if (solver instanceof DefaultSolver<ScheduleSolution> defaultSolver) {
			defaultSolver.addPhaseLifecycleListener(new ProgressListener());
		}

		// Add listener for best solution changes
		solver.addEventListener(this::onBestSolutionChanged);

		// Broadcast start event
		solverStatus.set(SolverStatus.SOLVING_ACTIVE);
		broadcastProgress(problem, "Solver started");

		// Run solver in background thread
		solverExecutor.submit(() -> {
			try {
				ScheduleSolution solution = solver.solve(problem);
				bestSolution.set(solution);
				solverStatus.set(SolverStatus.NOT_SOLVING);
				log.info("Solving finished with score: {}", solution.getScore());
				broadcastProgress(solution, "Solving complete");
			} catch (Exception e) {
				solverStatus.set(SolverStatus.NOT_SOLVING);
				log.error("Solver failed", e);
				broadcastProgress(bestSolution.get(), "Solver error: " + e.getMessage());
			} finally {
				currentSolver.set(null);
			}
		});

		return 1L;
	}

	/**
	 * Listener for step-by-step progress during solving.
	 */
	private class ProgressListener extends PhaseLifecycleListenerAdapter<ScheduleSolution> {
		@Override
		public void stepEnded(AbstractStepScope<ScheduleSolution> stepScope) {
			long now = System.currentTimeMillis();
			// Throttle broadcasts to avoid flooding
			if (now - lastBroadcastTime < BROADCAST_THROTTLE_MS) {
				return;
			}
			lastBroadcastTime = now;

			ScheduleSolution solution = stepScope.getWorkingSolution();
			if (solution != null) {
				// Update best solution reference for polling
				bestSolution.set(solution);
				broadcastProgress(solution, "Solving...");
			}
		}
	}

	/**
	 * Called when the solver finds a new best solution.
	 */
	private void onBestSolutionChanged(BestSolutionChangedEvent<ScheduleSolution> event) {
		if (event.isEveryProblemChangeProcessed()) {
			ScheduleSolution solution = event.getNewBestSolution();
			bestSolution.set(solution);
			log.debug("New best solution: {}", solution.getScore());
		}
	}

	/**
	 * Broadcast solver progress to WebSocket clients.
	 */
	private void broadcastProgress(ScheduleSolution solution, String message) {
		if (solution == null) {
			return;
		}

		var assignments = solution.getAssignments();
		if (assignments == null) {
			assignments = java.util.Collections.emptyList();
		}

		int assigned = (int) assignments.stream()
				.filter(ScheduleAssignment::isInitialized)
				.count();

		long elapsedSeconds = (System.currentTimeMillis() - solverStartTime) / 1000;

		SolverProgressEvent event = SolverProgressEvent.of(
				solverStatus.get(),
				solution.getScore(),
				assigned,
				assignments.size(),
				message,
				elapsedSeconds);

		try {
			messagingTemplate.convertAndSend(SOLVER_TOPIC, event);
		} catch (Exception e) {
			log.warn("Failed to broadcast solver progress: {}", e.getMessage());
		}
	}

	/**
	 * Get the current solver status.
	 */
	public SolverStatusResponse getStatus() {
		SolverStatus status = solverStatus.get();
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

	@Transactional(readOnly = true)
	public SolverAnalyticsResponse getAnalytics(String semester) {
		ScheduleSolution solution = bestSolution.get();
		if (shouldUseSolutionAnalytics(solution, semester)) {
			return buildAnalyticsFromSolution(solution);
		}
		return buildAnalyticsFromSavedSchedules(semester);
	}

	/**
	 * Stop the current solving process.
	 */
	public void stopSolving() {
		Solver<ScheduleSolution> solver = currentSolver.get();
		if (solver != null && solver.isSolving()) {
			log.info("Stopping solver");
			solver.terminateEarly();
			broadcastProgress(bestSolution.get(), "Solver stopped by user");
		}
		solverStatus.set(SolverStatus.NOT_SOLVING);
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
	 */
	private ScheduleSolution buildProblem(String semester) {
		List<Course> courses = courseRepository.findAll();
		List<Room> rooms = roomRepository.findAll();
		List<TimeSlot> timeSlots = timeSlotRepository.findAll();

		// Force initialization of lazy associations within transaction
		courses.forEach(course -> {
			if (course.getInstructor() != null) {
				course.getInstructor().getId();
			}
		});
		rooms.forEach(room -> {
			if (room.getBuilding() != null) {
				room.getBuilding().getId();
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
					SolverRoomDomainHelper.RoomDomain roomDomain = SolverRoomDomainHelper.buildRoomDomain(course, rooms);
					assignment.setAvailableRooms(roomDomain.allowedRooms());
					assignment.setPreferredBuildingCodes(roomDomain.preferredBuildingCodes());
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

	private boolean shouldUseSolutionAnalytics(ScheduleSolution solution, String semester) {
		if (solution == null || solution.getAssignments() == null || solution.getSemester() == null) {
			return false;
		}
		if (!solution.getSemester().equals(semester)) {
			return false;
		}
		if (solverStatus.get() == SolverStatus.SOLVING_ACTIVE) {
			return true;
		}
		long currentCourseCount = courseRepository.count();
		return solution.getAssignments().size() == currentCourseCount;
	}

	private SolverAnalyticsResponse buildAnalyticsFromSolution(ScheduleSolution solution) {
		List<Room> rooms = solution.getRooms() != null ? solution.getRooms() : roomRepository.findAll();
		List<TimeSlot> timeSlots = solution.getTimeSlots() != null ? solution.getTimeSlots() : timeSlotRepository.findAll();
		Map<Long, Long> scheduledByRoomId = new HashMap<>();
		Map<Long, Long> scheduledByTimeSlotId = new HashMap<>();

		for (ScheduleAssignment assignment : solution.getAssignments()) {
			if (!assignment.isInitialized()) {
				continue;
			}
			if (assignment.getRoom() != null && assignment.getRoom().getId() != null) {
				scheduledByRoomId.merge(assignment.getRoom().getId(), 1L, Long::sum);
			}
			if (assignment.getTimeSlot() != null && assignment.getTimeSlot().getId() != null) {
				scheduledByTimeSlotId.merge(assignment.getTimeSlot().getId(), 1L, Long::sum);
			}
		}

		return buildAnalyticsResponse(solution.getSemester(), rooms, timeSlots, scheduledByRoomId, scheduledByTimeSlotId);
	}

	private SolverAnalyticsResponse buildAnalyticsFromSavedSchedules(String semester) {
		List<Room> rooms = roomRepository.findAll();
		List<TimeSlot> timeSlots = timeSlotRepository.findAll();
		Map<Long, Long> scheduledByRoomId = new HashMap<>();
		Map<Long, Long> scheduledByTimeSlotId = new HashMap<>();

		for (Schedule schedule : scheduleRepository.findBySemester(semester)) {
			if (schedule.getRoom() != null && schedule.getRoom().getId() != null) {
				scheduledByRoomId.merge(schedule.getRoom().getId(), 1L, Long::sum);
			}
			if (schedule.getTimeSlot() != null && schedule.getTimeSlot().getId() != null) {
				scheduledByTimeSlotId.merge(schedule.getTimeSlot().getId(), 1L, Long::sum);
			}
		}

		return buildAnalyticsResponse(semester, rooms, timeSlots, scheduledByRoomId, scheduledByTimeSlotId);
	}

	private SolverAnalyticsResponse buildAnalyticsResponse(
			String semester,
			List<Room> rooms,
			List<TimeSlot> timeSlots,
			Map<Long, Long> scheduledByRoomId,
			Map<Long, Long> scheduledByTimeSlotId) {

		int roomCount = rooms != null ? rooms.size() : 0;
		int timeSlotCount = timeSlots != null ? timeSlots.size() : 0;
		long totalAvailableSlots = (long) roomCount * timeSlotCount;
		long totalScheduledSlots = scheduledByRoomId.values().stream().mapToLong(Long::longValue).sum();
		double overallUtilization = totalAvailableSlots > 0
				? (double) totalScheduledSlots / totalAvailableSlots * 100.0
				: 0.0;

		List<SolverRoomUtilization> roomUtilization = (rooms != null ? rooms : List.<Room>of()).stream()
				.map(room -> {
					long scheduledSlots = scheduledByRoomId.getOrDefault(room.getId(), 0L);
					long totalSlots = timeSlotCount;
					double utilization = totalSlots > 0 ? (double) scheduledSlots / totalSlots * 100.0 : 0.0;
					return new SolverRoomUtilization(
							room.getId(),
							room.getRoomNumber(),
							room.getBuildingName(),
							room.getBuildingCode(),
							room.getCapacity(),
							scheduledSlots,
							totalSlots,
							utilization);
				})
				.toList();

		List<SolverRoomUtilization> sortedRooms = roomUtilization.stream()
				.sorted(Comparator.comparing(SolverRoomUtilization::utilizationPercentage).reversed())
				.toList();

		Map<Long, List<Room>> roomsByBuilding = (rooms != null ? rooms : List.<Room>of()).stream()
				.filter(room -> room.getBuildingId() != null)
				.collect(Collectors.groupingBy(Room::getBuildingId, LinkedHashMap::new, Collectors.toList()));

		List<SolverBuildingUtilization> buildingUtilization = roomsByBuilding.entrySet().stream()
				.map(entry -> {
					Long buildingId = entry.getKey();
					List<Room> buildingRooms = entry.getValue();
					int buildingRoomCount = buildingRooms.size();
					long buildingScheduledSlots = buildingRooms.stream()
							.map(Room::getId)
							.mapToLong(roomId -> scheduledByRoomId.getOrDefault(roomId, 0L))
							.sum();
					long buildingTotalSlots = (long) buildingRoomCount * timeSlotCount;
					double utilization = buildingTotalSlots > 0
							? (double) buildingScheduledSlots / buildingTotalSlots * 100.0
							: 0.0;
					Room sample = buildingRooms.getFirst();
					return new SolverBuildingUtilization(
							buildingId,
							sample.getBuildingName(),
							sample.getBuildingCode(),
							buildingRoomCount,
							buildingScheduledSlots,
							buildingTotalSlots,
							utilization);
				})
				.sorted(Comparator.comparing(SolverBuildingUtilization::utilizationPercentage).reversed())
				.toList();

		List<SolverPeakHours> peakHours = (timeSlots != null ? timeSlots : List.<TimeSlot>of()).stream()
				.map(slot -> new SolverPeakHours(
						slot.getId(),
						slot.getDayOfWeek(),
						slot.getStartTime(),
						slot.getEndTime(),
						slot.getLabel(),
						scheduledByTimeSlotId.getOrDefault(slot.getId(), 0L)))
				.sorted(Comparator.comparing(SolverPeakHours::bookingCount).reversed())
				.toList();

		List<SolverRoomUtilization> topUtilized = sortedRooms.stream().limit(5).toList();
		List<SolverRoomUtilization> leastUtilized = roomUtilization.stream()
				.sorted(Comparator.comparing(SolverRoomUtilization::utilizationPercentage))
				.limit(5)
				.toList();

		return new SolverAnalyticsResponse(
				semester,
				roomCount,
				buildingUtilization.size(),
				totalScheduledSlots,
				totalAvailableSlots,
				overallUtilization,
				topUtilized,
				leastUtilized,
				roomUtilization,
				buildingUtilization,
				peakHours);
	}
}
