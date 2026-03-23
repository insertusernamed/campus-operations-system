package org.campusscheduler.solver;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentAssignmentService;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleSeatLimitResolver;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.student.StudentRepository;
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
	private final EnrollmentRepository enrollmentRepository;
	private final StudentRepository studentRepository;
	private final EnrollmentAssignmentService enrollmentAssignmentService;
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
			EnrollmentRepository enrollmentRepository,
			StudentRepository studentRepository,
			EnrollmentAssignmentService enrollmentAssignmentService,
			SimpMessagingTemplate messagingTemplate) {
		this.solverConfig = solverConfig;
		this.courseRepository = courseRepository;
		this.roomRepository = roomRepository;
		this.timeSlotRepository = timeSlotRepository;
		this.scheduleRepository = scheduleRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.studentRepository = studentRepository;
		this.enrollmentAssignmentService = enrollmentAssignmentService;
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

	public record SolverStudentDailyLoad(
			int classesPerDay,
			long studentDays) {
	}

	public record SolverAnalyticsResponse(
			String semester,
			int totalRooms,
			int totalBuildings,
			long totalScheduledSlots,
			long totalAvailableSlots,
			double overallUtilizationPercentage,
			int totalStudents,
			long enrolledRequests,
			long waitlistedRequests,
			double averageFillRate,
			double averageGapMinutes,
			List<SolverStudentDailyLoad> dailyLoadDistribution,
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
		enrollmentRepository.deleteBySemester(semester);
		scheduleRepository.deleteBySemester(semester);

		List<Schedule> schedulesToSave = solution.getAssignments().stream()
				.filter(ScheduleAssignment::isInitialized)
				.map(assignment -> Schedule.builder()
						.course(assignment.getCourse())
						.room(assignment.getRoom())
						.timeSlot(assignment.getTimeSlot())
						.semester(semester)
						.build())
				.toList();
		List<Schedule> savedSchedules = scheduleRepository.saveAll(schedulesToSave);

		List<Student> students = studentRepository.findAll();
		List<Enrollment> savedEnrollments = enrollmentRepository.saveAll(
				enrollmentAssignmentService.assignEnrollments(students, savedSchedules, semester));

		log.info("Saved {} schedules and {} enrollments for semester {}",
				savedSchedules.size(), savedEnrollments.size(), semester);
		return savedSchedules.size();
	}

	/**
	 * Build the initial problem from database data.
	 */
	private ScheduleSolution buildProblem(String semester) {
		List<Course> courses = courseRepository.findAll();
		List<Room> rooms = roomRepository.findAll();
		List<TimeSlot> timeSlots = timeSlotRepository.findAll();
		List<Student> students = studentRepository.findAll();

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
		students.forEach(student -> student.getPreferredCourseIds().size());

		log.info("Building problem: {} courses, {} rooms, {} time slots",
				courses.size(), rooms.size(), timeSlots.size());

		List<StudentCourseDemand> studentCourseDemands = buildStudentCourseDemands(students, courses);
		List<CourseDemandSummary> courseDemandSummaries = buildCourseDemandSummaries(studentCourseDemands);

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
				.studentCourseDemands(studentCourseDemands)
				.courseDemandSummaries(courseDemandSummaries)
				.assignments(assignments)
				.semester(semester)
				.build();
	}

	private List<StudentCourseDemand> buildStudentCourseDemands(List<Student> students, List<Course> courses) {
		Set<Long> availableCourseIds = courses.stream()
				.map(Course::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		AtomicLong nextDemandId = new AtomicLong(1L);

		return students.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator
						.comparing((Student student) -> normalize(student.getStudentNumber()))
						.thenComparing(student -> normalize(student.getEmail()))
						.thenComparing(student -> student.getId() == null ? Long.MAX_VALUE : student.getId()))
				.flatMap(student -> toDemandFacts(student, availableCourseIds, nextDemandId).stream())
				.toList();
	}

	private List<StudentCourseDemand> toDemandFacts(
			Student student,
			Set<Long> availableCourseIds,
			AtomicLong nextDemandId) {
		if (student.getId() == null || student.getPreferredCourseIds() == null || student.getPreferredCourseIds().isEmpty()) {
			return List.of();
		}

		List<Long> uniquePreferredCourseIds = student.getPreferredCourseIds().stream()
				.filter(Objects::nonNull)
				.filter(availableCourseIds::contains)
				.distinct()
				.toList();
		if (uniquePreferredCourseIds.isEmpty()) {
			return List.of();
		}

		int targetCourseLoad = Math.max(1,
				Math.min(student.getTargetCourseLoad() == null ? uniquePreferredCourseIds.size() : student.getTargetCourseLoad(),
						uniquePreferredCourseIds.size()));
		int highPriorityCutoff = Math.min(targetCourseLoad, 2);

		return java.util.stream.IntStream.range(0, uniquePreferredCourseIds.size())
				.mapToObj(index -> new StudentCourseDemand(
						nextDemandId.getAndIncrement(),
						student.getId(),
						uniquePreferredCourseIds.get(index),
						index,
						targetCourseLoad,
						index < targetCourseLoad,
						index < highPriorityCutoff))
				.toList();
	}

	private List<CourseDemandSummary> buildCourseDemandSummaries(List<StudentCourseDemand> studentCourseDemands) {
		Map<Long, DemandAccumulator> demandByCourseId = new LinkedHashMap<>();

		for (StudentCourseDemand demand : studentCourseDemands) {
			DemandAccumulator accumulator = demandByCourseId.computeIfAbsent(
					demand.courseId(),
					ignored -> new DemandAccumulator());
			accumulator.totalRequestCount++;
			if (demand.primaryRequest()) {
				accumulator.primaryRequestCount++;
			}
			if (demand.highPriorityRequest()) {
				accumulator.highPriorityRequestCount++;
			}
		}

		return demandByCourseId.entrySet().stream()
				.map(entry -> new CourseDemandSummary(
						entry.getKey(),
						entry.getValue().totalRequestCount,
						entry.getValue().primaryRequestCount,
						entry.getValue().highPriorityRequestCount))
				.toList();
	}

	private String normalize(String value) {
		return value == null ? "" : value;
	}

	private static final class DemandAccumulator {
		private int totalRequestCount;
		private int primaryRequestCount;
		private int highPriorityRequestCount;
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
		List<Schedule> simulatedSchedules = solution.getAssignments().stream()
				.filter(ScheduleAssignment::isInitialized)
				.map(this::toAnalyticsSchedule)
				.toList();
		List<Student> students = loadStudentsForAnalytics();
		List<Enrollment> enrollments = enrollmentAssignmentService.assignEnrollments(
				students,
				simulatedSchedules,
				solution.getSemester());
		StudentAnalyticsMetrics studentMetrics = buildStudentAnalyticsMetrics(students, simulatedSchedules, enrollments);
		Map<Long, Long> scheduledByRoomId = new HashMap<>();
		Map<Long, Long> scheduledByTimeSlotId = new HashMap<>();

		for (Schedule schedule : simulatedSchedules) {
			Room assignedRoom = schedule.getRoom();
			TimeSlot assignedTimeSlot = schedule.getTimeSlot();
			if (assignedRoom == null || assignedTimeSlot == null) {
				continue;
			}

			if (assignedRoom.getId() != null) {
				scheduledByRoomId.merge(assignedRoom.getId(), 1L, Long::sum);
			}
			if (assignedTimeSlot.getId() != null) {
				scheduledByTimeSlotId.merge(assignedTimeSlot.getId(), 1L, Long::sum);
			}
		}

		return buildAnalyticsResponse(
				solution.getSemester(),
				rooms,
				timeSlots,
				scheduledByRoomId,
				scheduledByTimeSlotId,
				studentMetrics);
	}

	private SolverAnalyticsResponse buildAnalyticsFromSavedSchedules(String semester) {
		List<Room> rooms = roomRepository.findAll();
		List<TimeSlot> timeSlots = timeSlotRepository.findAll();
		List<Schedule> schedules = scheduleRepository.findBySemester(semester);
		List<Student> students = loadStudentsForAnalytics();
		List<Enrollment> enrollments = enrollmentRepository.findBySemester(semester);
		if (enrollments == null) {
			enrollments = List.of();
		}
		StudentAnalyticsMetrics studentMetrics = buildStudentAnalyticsMetrics(students, schedules, enrollments);
		Map<Long, Long> scheduledByRoomId = new HashMap<>();
		Map<Long, Long> scheduledByTimeSlotId = new HashMap<>();

		for (Schedule schedule : schedules) {
			if (schedule.getRoom() != null && schedule.getRoom().getId() != null) {
				scheduledByRoomId.merge(schedule.getRoom().getId(), 1L, Long::sum);
			}
			if (schedule.getTimeSlot() != null && schedule.getTimeSlot().getId() != null) {
				scheduledByTimeSlotId.merge(schedule.getTimeSlot().getId(), 1L, Long::sum);
			}
		}

		return buildAnalyticsResponse(
				semester,
				rooms,
				timeSlots,
				scheduledByRoomId,
				scheduledByTimeSlotId,
				studentMetrics);
	}

	private SolverAnalyticsResponse buildAnalyticsResponse(
			String semester,
			List<Room> rooms,
			List<TimeSlot> timeSlots,
			Map<Long, Long> scheduledByRoomId,
			Map<Long, Long> scheduledByTimeSlotId,
			StudentAnalyticsMetrics studentMetrics) {

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
				studentMetrics.totalStudents(),
				studentMetrics.enrolledRequests(),
				studentMetrics.waitlistedRequests(),
				studentMetrics.averageFillRate(),
				studentMetrics.averageGapMinutes(),
				studentMetrics.dailyLoadDistribution(),
				topUtilized,
				leastUtilized,
				roomUtilization,
				buildingUtilization,
				peakHours);
	}

	private Schedule toAnalyticsSchedule(ScheduleAssignment assignment) {
		return Schedule.builder()
				.id(assignment.getId())
				.course(assignment.getCourse())
				.room(assignment.getRoom())
				.timeSlot(assignment.getTimeSlot())
				.semester(assignment.getSemester())
				.build();
	}

	private List<Student> loadStudentsForAnalytics() {
		List<Student> students = studentRepository.findAll();
		if (students == null) {
			return List.of();
		}
		students.forEach(student -> {
			if (student.getPreferredCourseIds() != null) {
				student.getPreferredCourseIds().size();
			}
		});
		return students;
	}

	private StudentAnalyticsMetrics buildStudentAnalyticsMetrics(
			List<Student> students,
			List<Schedule> schedules,
			List<Enrollment> enrollments) {
		List<Student> safeStudents = students != null ? students : List.of();
		List<Schedule> safeSchedules = schedules != null ? schedules : List.of();
		List<Enrollment> safeEnrollments = enrollments != null ? enrollments : List.of();

		long enrolledRequests = safeEnrollments.stream()
				.filter(enrollment -> enrollment.getStatus() == org.campusscheduler.domain.enrollment.EnrollmentStatus.ENROLLED)
				.count();
		long waitlistedRequests = safeEnrollments.stream()
				.filter(enrollment -> enrollment.getStatus() == org.campusscheduler.domain.enrollment.EnrollmentStatus.WAITLISTED)
				.count();

		Map<Long, Long> filledSeatsByScheduleId = safeEnrollments.stream()
				.filter(enrollment -> enrollment.getStatus() == org.campusscheduler.domain.enrollment.EnrollmentStatus.ENROLLED)
				.filter(enrollment -> enrollment.getSchedule() != null && enrollment.getSchedule().getId() != null)
				.collect(Collectors.groupingBy(
						enrollment -> enrollment.getSchedule().getId(),
						LinkedHashMap::new,
						Collectors.counting()));

		double averageFillRate = safeSchedules.stream()
				.mapToDouble(schedule -> {
					int seatLimit = resolveSeatLimit(schedule);
					if (seatLimit <= 0) {
						return 0.0;
					}
					return filledSeatsByScheduleId.getOrDefault(schedule.getId(), 0L) * 100.0 / seatLimit;
				})
				.average()
				.orElse(0.0);

		double averageGapMinutes = computeAverageGapMinutes(safeEnrollments);
		List<SolverStudentDailyLoad> dailyLoadDistribution = buildDailyLoadDistribution(safeEnrollments);

		return new StudentAnalyticsMetrics(
				safeStudents.size(),
				enrolledRequests,
				waitlistedRequests,
				averageFillRate,
				averageGapMinutes,
				dailyLoadDistribution);
	}

	private double computeAverageGapMinutes(List<Enrollment> enrollments) {
		List<Enrollment> enrolledOnly = enrollments.stream()
				.filter(enrollment -> enrollment.getStatus() == org.campusscheduler.domain.enrollment.EnrollmentStatus.ENROLLED)
				.filter(enrollment -> enrollment.getStudent() != null && enrollment.getStudent().getId() != null)
				.filter(enrollment -> enrollment.getSchedule() != null && enrollment.getSchedule().getTimeSlot() != null)
				.toList();

		Map<StudentDayBucket, List<Enrollment>> byStudentDay = enrolledOnly.stream()
				.filter(enrollment -> enrollment.getSchedule().getTimeSlot().getDayOfWeek() != null)
				.collect(Collectors.groupingBy(
						enrollment -> new StudentDayBucket(
								enrollment.getStudent().getId(),
								enrollment.getSchedule().getTimeSlot().getDayOfWeek()),
						LinkedHashMap::new,
						Collectors.toList()));

		long totalGapMinutes = 0;
		long gapCount = 0;
		for (List<Enrollment> dayEnrollments : byStudentDay.values()) {
			List<Enrollment> sorted = dayEnrollments.stream()
					.sorted(Comparator.comparing(
							enrollment -> enrollment.getSchedule().getTimeSlot().getStartTime(),
							Comparator.nullsLast(Comparator.naturalOrder())))
					.toList();
			for (int index = 1; index < sorted.size(); index++) {
				LocalTime previousEnd = sorted.get(index - 1).getSchedule().getTimeSlot().getEndTime();
				LocalTime currentStart = sorted.get(index).getSchedule().getTimeSlot().getStartTime();
				if (previousEnd == null || currentStart == null || !currentStart.isAfter(previousEnd)) {
					continue;
				}
				totalGapMinutes += Duration.between(previousEnd, currentStart).toMinutes();
				gapCount++;
			}
		}

		return gapCount > 0 ? (double) totalGapMinutes / gapCount : 0.0;
	}

	private List<SolverStudentDailyLoad> buildDailyLoadDistribution(List<Enrollment> enrollments) {
		Map<StudentDayBucket, Long> classesByStudentDay = enrollments.stream()
				.filter(enrollment -> enrollment.getStatus() == org.campusscheduler.domain.enrollment.EnrollmentStatus.ENROLLED)
				.filter(enrollment -> enrollment.getStudent() != null && enrollment.getStudent().getId() != null)
				.filter(enrollment -> enrollment.getSchedule() != null && enrollment.getSchedule().getTimeSlot() != null)
				.filter(enrollment -> enrollment.getSchedule().getTimeSlot().getDayOfWeek() != null)
				.collect(Collectors.groupingBy(
						enrollment -> new StudentDayBucket(
								enrollment.getStudent().getId(),
								enrollment.getSchedule().getTimeSlot().getDayOfWeek()),
						LinkedHashMap::new,
						Collectors.counting()));

		return classesByStudentDay.values().stream()
				.collect(Collectors.groupingBy(
						Long::intValue,
						LinkedHashMap::new,
						Collectors.counting()))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new SolverStudentDailyLoad(entry.getKey(), entry.getValue()))
				.toList();
	}

	private int resolveSeatLimit(Schedule schedule) {
		return ScheduleSeatLimitResolver.resolve(schedule);
	}

	private record StudentAnalyticsMetrics(
			int totalStudents,
			long enrolledRequests,
			long waitlistedRequests,
			double averageFillRate,
			double averageGapMinutes,
			List<SolverStudentDailyLoad> dailyLoadDistribution) {
	}

	private record StudentDayBucket(Long studentId, DayOfWeek dayOfWeek) {
	}
}
