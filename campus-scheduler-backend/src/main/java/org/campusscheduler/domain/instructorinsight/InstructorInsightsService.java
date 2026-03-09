package org.campusscheduler.domain.instructorinsight;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.changerequest.ScheduleChangeRequest;
import org.campusscheduler.domain.changerequest.ScheduleChangeRequestRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreference;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceService;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceSettings;
import org.campusscheduler.domain.instructorpreference.RoomFeatureCatalog;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Computes proactive friction issues and admin-focused operational insights for instructors.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorInsightsService {

    private static final int TARGET_CREDITS_MIN = 6;
    private static final int TARGET_CREDITS_MAX = 12;
    private static final int PREFERENCE_COMPLETENESS_THRESHOLD = 70;
    private static final int FRICTION_HOTSPOT_SCORE = 8;

    private static final List<DayOfWeek> ORDERED_DAYS = List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY);

    private final ScheduleRepository scheduleRepository;
    private final InstructorPreferenceService instructorPreferenceService;
    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final InstructorPreferenceRepository instructorPreferenceRepository;
    private final ScheduleChangeRequestRepository changeRequestRepository;

    public InstructorInsightsSummaryResponse getSummary(String semester) {
        InsightsSnapshot snapshot = buildSnapshot(semester);

        long noCurrentAssignment = snapshot.queueRows().stream()
                .filter(row -> row.assignedCoursesCount() == 0)
                .count();

        long overloadRisk = snapshot.queueRows().stream()
                .filter(row -> row.loadStatus() == InstructorLoadStatus.OVER)
                .count();

        long preferenceSetupIncomplete = snapshot.queueRows().stream()
                .filter(row -> row.preferenceCompletenessPercent() < PREFERENCE_COMPLETENESS_THRESHOLD)
                .count();

        long frictionHotspots = snapshot.queueRows().stream()
                .filter(this::isFrictionHotspot)
                .count();

        long departmentsWithCoverageRisk = snapshot.departmentLoads().stream()
                .filter(department -> department.coverageRiskLevel() != InstructorCoverageRiskLevel.LOW)
                .count();

        return new InstructorInsightsSummaryResponse(
                snapshot.queueRows().size(),
                noCurrentAssignment,
                overloadRisk,
                preferenceSetupIncomplete,
                frictionHotspots,
                departmentsWithCoverageRisk);
    }

    public List<InstructorQueueRowResponse> getQueue(String semester, String filter, String department) {
        InsightsSnapshot snapshot = buildSnapshot(semester);
        QueueFilter queueFilter = parseQueueFilter(filter);
        String departmentFilter = normalizeDepartmentFilter(department);

        return snapshot.queueRows().stream()
                .filter(row -> matchesQueueFilter(row, queueFilter))
                .filter(row -> departmentFilter == null || row.department().equalsIgnoreCase(departmentFilter))
                .sorted(queueComparator())
                .toList();
    }

    public InstructorLoadDistributionResponse getLoadDistribution(String semester) {
        InsightsSnapshot snapshot = buildSnapshot(semester);
        return new InstructorLoadDistributionResponse(semester, snapshot.departmentLoads());
    }

    public Optional<InstructorWorkbenchResponse> getWorkbench(Long instructorId, String semester) {
        Optional<Instructor> instructorOpt = instructorRepository.findById(instructorId);
        if (instructorOpt.isEmpty()) {
            return Optional.empty();
        }

        InsightsSnapshot snapshot = buildSnapshot(semester);
        Optional<InstructorQueueRowResponse> queueRowOpt = snapshot.queueRows().stream()
                .filter(row -> row.id().equals(instructorId))
                .findFirst();

        if (queueRowOpt.isEmpty()) {
            return Optional.empty();
        }

        Instructor instructor = instructorOpt.get();
        InstructorQueueRowResponse queueRow = queueRowOpt.get();

        List<Course> assignedCourses = courseRepository.findByInstructorId(instructorId);
        List<Schedule> semesterSchedules = scheduleRepository.findByCourseInstructorIdAndSemester(instructorId, semester);

        List<InstructorFrictionIssueResponse> frictions = snapshot.frictionByInstructor()
                .getOrDefault(instructorId, List.of());

        InstructorFrictionSummaryResponse frictionSummary = summarizeFriction(frictions);

        InstructorWorkbenchResponse response = new InstructorWorkbenchResponse(
                instructorId,
                instructor.getFirstName(),
                instructor.getLastName(),
                instructor.getEmail(),
                normalizeDepartment(instructor.getDepartment()),
                instructor.getOfficeNumber(),
                semester,
                queueRow.assignedCoursesCount(),
                queueRow.assignedCredits(),
                queueRow.targetCreditsMin(),
                queueRow.targetCreditsMax(),
                queueRow.loadStatus(),
                queueRow.preferenceCompletenessPercent(),
                queueRow.frictionScore(),
                frictionSummary,
                buildLoadTrend(instructorId, semester),
                buildWeeklyDensity(semesterSchedules),
                buildAssignedCourseContext(assignedCourses, semesterSchedules, semester),
                frictions,
                buildRecentChanges(instructorId, semester),
                queueRow.recommendedActions());

        return Optional.of(response);
    }

    public Optional<List<InstructorFrictionIssueResponse>> findFrictions(Long instructorId, String semester) {
        Optional<InstructorPreferenceSettings> settingsOpt = instructorPreferenceService.getEffectiveSettings(instructorId);
        if (settingsOpt.isEmpty()) {
            return Optional.empty();
        }

        List<Schedule> schedules = scheduleRepository.findByCourseInstructorIdAndSemester(instructorId, semester);
        return Optional.of(computeFrictions(settingsOpt.get(), schedules));
    }

    private InsightsSnapshot buildSnapshot(String semester) {
        List<Instructor> instructors = instructorRepository.findAll();
        List<Course> allCourses = courseRepository.findAll();
        List<Schedule> semesterSchedules = scheduleRepository.findBySemester(semester);

        Map<Long, InstructorPreference> explicitPreferencesByInstructor = new HashMap<>();
        for (InstructorPreference preference : instructorPreferenceRepository.findAll()) {
            if (preference.getInstructor() == null || preference.getInstructor().getId() == null) {
                continue;
            }
            explicitPreferencesByInstructor.put(preference.getInstructor().getId(), preference);
        }

        Map<Long, List<Course>> coursesByInstructor = new HashMap<>();
        Map<String, DepartmentDemand> unfilledDemandByDepartment = new HashMap<>();

        for (Course course : allCourses) {
            String department = normalizeDepartment(course.getDepartment());
            if (course.getInstructor() != null && course.getInstructor().getId() != null) {
                Long instructorId = course.getInstructor().getId();
                coursesByInstructor.computeIfAbsent(instructorId, ignored -> new ArrayList<>()).add(course);
                continue;
            }

            DepartmentDemand demand = unfilledDemandByDepartment.computeIfAbsent(department, ignored -> new DepartmentDemand());
            demand.unfilledCourseCount += 1;
            demand.unfilledCredits += safeCredits(course);
        }

        Map<Long, List<Schedule>> schedulesByInstructor = new HashMap<>();
        for (Schedule schedule : semesterSchedules) {
            Long instructorId = extractInstructorId(schedule);
            if (instructorId == null) {
                continue;
            }
            schedulesByInstructor.computeIfAbsent(instructorId, ignored -> new ArrayList<>()).add(schedule);
        }

        Map<String, Integer> instructorCountByDepartment = new HashMap<>();
        for (Instructor instructor : instructors) {
            String department = normalizeDepartment(instructor.getDepartment());
            instructorCountByDepartment.merge(department, 1, Integer::sum);
        }

        Set<String> allDepartments = new LinkedHashSet<>();
        allDepartments.addAll(instructorCountByDepartment.keySet());
        allDepartments.addAll(unfilledDemandByDepartment.keySet());

        Map<String, InstructorCoverageRiskLevel> departmentCoverageRisk = new HashMap<>();
        for (String department : allDepartments) {
            DepartmentDemand demand = unfilledDemandByDepartment.getOrDefault(department, DepartmentDemand.EMPTY);
            departmentCoverageRisk.put(
                    department,
                    determineDepartmentCoverageRisk(demand.unfilledCourseCount, demand.unfilledCredits));
        }

        Map<Long, List<InstructorFrictionIssueResponse>> frictionByInstructor = new HashMap<>();
        List<InstructorQueueRowResponse> queueRows = new ArrayList<>();

        for (Instructor instructor : instructors) {
            Long instructorId = instructor.getId();
            List<Course> assignedCourses = coursesByInstructor.getOrDefault(instructorId, List.of());
            int assignedCredits = assignedCourses.stream().mapToInt(this::safeCredits).sum();
            int assignedCourseCount = assignedCourses.size();
            InstructorLoadStatus loadStatus = determineLoadStatus(assignedCredits);
            int overloadCredits = Math.max(0, assignedCredits - TARGET_CREDITS_MAX);
            int underUtilizedCredits = Math.max(0, TARGET_CREDITS_MIN - assignedCredits);

            InstructorPreference explicitPreference = explicitPreferencesByInstructor.get(instructorId);
            int preferenceCompleteness = computePreferenceCompleteness(explicitPreference);
            InstructorPreferenceSettings settings = toSettings(explicitPreference);

            List<Schedule> instructorSchedules = schedulesByInstructor.getOrDefault(instructorId, List.of());
            List<InstructorFrictionIssueResponse> frictions = computeFrictions(settings, instructorSchedules);
            frictionByInstructor.put(instructorId, frictions);

            int frictionScore = computeFrictionScore(frictions);
            InstructorFrictionSeverity frictionSeverity = determineFrictionSeverity(frictions, frictionScore);

            String department = normalizeDepartment(instructor.getDepartment());
            InstructorCoverageRiskLevel coverageRiskLevel = determineInstructorCoverageRisk(
                    departmentCoverageRisk.getOrDefault(department, InstructorCoverageRiskLevel.LOW),
                    loadStatus);

            InstructorOperationalStatus status = determineOperationalStatus(
                    coverageRiskLevel,
                    loadStatus,
                    preferenceCompleteness,
                    frictionSeverity,
                    frictionScore);

            List<String> recommendedActions = buildRecommendedActions(
                    assignedCourseCount,
                    loadStatus,
                    preferenceCompleteness,
                    frictions,
                    coverageRiskLevel,
                    status);

            queueRows.add(new InstructorQueueRowResponse(
                    instructorId,
                    instructor.getFirstName(),
                    instructor.getLastName(),
                    instructor.getFirstName() + " " + instructor.getLastName(),
                    instructor.getEmail(),
                    department,
                    assignedCourseCount,
                    assignedCredits,
                    TARGET_CREDITS_MIN,
                    TARGET_CREDITS_MAX,
                    loadStatus,
                    preferenceCompleteness,
                    frictionScore,
                    frictions.size(),
                    frictionSeverity,
                    coverageRiskLevel,
                    status,
                    overloadCredits,
                    underUtilizedCredits,
                    recommendedActions));
        }

        Map<String, Integer> assignedCreditsByDepartment = new HashMap<>();
        for (InstructorQueueRowResponse row : queueRows) {
            assignedCreditsByDepartment.merge(row.department(), row.assignedCredits(), Integer::sum);
        }

        List<InstructorDepartmentLoadResponse> departmentLoads = new ArrayList<>();
        for (String department : allDepartments) {
            int instructorCount = instructorCountByDepartment.getOrDefault(department, 0);
            DepartmentDemand demand = unfilledDemandByDepartment.getOrDefault(department, DepartmentDemand.EMPTY);
            departmentLoads.add(new InstructorDepartmentLoadResponse(
                    department,
                    instructorCount,
                    assignedCreditsByDepartment.getOrDefault(department, 0),
                    instructorCount * TARGET_CREDITS_MIN,
                    instructorCount * TARGET_CREDITS_MAX,
                    demand.unfilledCourseCount,
                    demand.unfilledCredits,
                    departmentCoverageRisk.getOrDefault(department, InstructorCoverageRiskLevel.LOW)));
        }

        departmentLoads.sort(Comparator
                .comparing((InstructorDepartmentLoadResponse row) -> coverageRiskRank(row.coverageRiskLevel()))
                .reversed()
                .thenComparing(InstructorDepartmentLoadResponse::unfilledCredits, Comparator.reverseOrder())
                .thenComparing(InstructorDepartmentLoadResponse::department));

        return new InsightsSnapshot(
                List.copyOf(queueRows),
                List.copyOf(departmentLoads),
                Map.copyOf(frictionByInstructor));
    }

    private List<InstructorAssignedCourseContextResponse> buildAssignedCourseContext(
            List<Course> assignedCourses,
            List<Schedule> semesterSchedules,
            String semester) {
        Map<Long, List<Schedule>> schedulesByCourse = new HashMap<>();
        for (Schedule schedule : semesterSchedules) {
            if (schedule.getCourse() == null || schedule.getCourse().getId() == null) {
                continue;
            }
            schedulesByCourse.computeIfAbsent(schedule.getCourse().getId(), ignored -> new ArrayList<>()).add(schedule);
        }

        List<InstructorAssignedCourseContextResponse> rows = new ArrayList<>();

        for (Course course : assignedCourses) {
            List<Schedule> scheduleMatches = schedulesByCourse.getOrDefault(course.getId(), List.of());
            if (scheduleMatches.isEmpty()) {
                rows.add(new InstructorAssignedCourseContextResponse(
                        course.getId(),
                        course.getCode(),
                        course.getName(),
                        safeCredits(course),
                        safeEnrollmentCapacity(course),
                        false,
                        null,
                        null,
                        null,
                        null,
                        null));
                continue;
            }

            for (Schedule schedule : scheduleMatches) {
                String day = schedule.getTimeSlot() != null && schedule.getTimeSlot().getDayOfWeek() != null
                        ? schedule.getTimeSlot().getDayOfWeek().name()
                        : null;
                String start = schedule.getTimeSlot() != null && schedule.getTimeSlot().getStartTime() != null
                        ? schedule.getTimeSlot().getStartTime().toString()
                        : null;
                String end = schedule.getTimeSlot() != null && schedule.getTimeSlot().getEndTime() != null
                        ? schedule.getTimeSlot().getEndTime().toString()
                        : null;

                rows.add(new InstructorAssignedCourseContextResponse(
                        course.getId(),
                        course.getCode(),
                        course.getName(),
                        safeCredits(course),
                        safeEnrollmentCapacity(course),
                        true,
                        day,
                        start,
                        end,
                        roomLabel(schedule),
                        semester));
            }
        }

        rows.sort(Comparator
                .comparing(InstructorAssignedCourseContextResponse::scheduled).reversed()
                .thenComparing(item -> item.dayOfWeek() == null ? "ZZZ" : item.dayOfWeek())
                .thenComparing(item -> item.startTime() == null ? "99:99" : item.startTime())
                .thenComparing(item -> item.code() == null ? "" : item.code()));

        return rows;
    }

    private List<InstructorRecentChangeResponse> buildRecentChanges(Long instructorId, String semester) {
        List<ScheduleChangeRequest> requests = changeRequestRepository.findByFilters(null, instructorId, semester, null);
        List<TimedChange> changes = new ArrayList<>();

        for (ScheduleChangeRequest request : requests) {
            String courseCode = request.getSchedule() != null && request.getSchedule().getCourse() != null
                    ? request.getSchedule().getCourse().getCode()
                    : "course";

            if (request.getCreatedAt() != null) {
                changes.add(new TimedChange(
                        request.getCreatedAt(),
                        "Requested change for " + courseCode,
                        "REQUEST"));
            }

            if (request.getReviewedAt() != null) {
                changes.add(new TimedChange(
                        request.getReviewedAt(),
                        "Request #" + request.getId() + " reviewed: " + request.getStatus(),
                        "REVIEW"));
            }

            if (request.getAppliedAt() != null) {
                changes.add(new TimedChange(
                        request.getAppliedAt(),
                        "Applied approved change for " + courseCode,
                        "APPLY"));
            }
        }

        return changes.stream()
                .sorted(Comparator.comparing(TimedChange::timestamp).reversed())
                .limit(5)
                .map(change -> new InstructorRecentChangeResponse(
                        change.timestamp().toString(),
                        change.label(),
                        change.source()))
                .toList();
    }

    private InstructorLoadTrendResponse buildLoadTrend(Long instructorId, String semester) {
        List<Schedule> allInstructorSchedules = scheduleRepository.findByCourseInstructorId(instructorId);
        Map<String, Map<Long, Integer>> creditsBySemesterAndCourse = new HashMap<>();

        for (Schedule schedule : allInstructorSchedules) {
            if (schedule.getSemester() == null || schedule.getCourse() == null) {
                continue;
            }

            Long courseId = schedule.getCourse().getId();
            if (courseId == null) {
                continue;
            }

            creditsBySemesterAndCourse
                    .computeIfAbsent(schedule.getSemester(), ignored -> new HashMap<>())
                    .put(courseId, safeCredits(schedule.getCourse()));
        }

        Map<String, Integer> creditsBySemester = new HashMap<>();
        for (Map.Entry<String, Map<Long, Integer>> entry : creditsBySemesterAndCourse.entrySet()) {
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            creditsBySemester.put(entry.getKey(), total);
        }

        int currentCredits = creditsBySemester.getOrDefault(semester, 0);

        List<Integer> baselineValues = creditsBySemester.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(semester))
                .map(Map.Entry::getValue)
                .toList();

        double baseline = baselineValues.isEmpty()
                ? (TARGET_CREDITS_MIN + TARGET_CREDITS_MAX) / 2.0
                : baselineValues.stream().mapToInt(Integer::intValue).average().orElse((TARGET_CREDITS_MIN + TARGET_CREDITS_MAX) / 2.0);

        double delta = currentCredits - baseline;
        String direction;
        if (Math.abs(delta) < 0.5) {
            direction = "FLAT";
        } else if (delta > 0) {
            direction = "UP";
        } else {
            direction = "DOWN";
        }

        return new InstructorLoadTrendResponse(
                currentCredits,
                roundToOneDecimal(baseline),
                roundToOneDecimal(delta),
                direction);
    }

    private List<InstructorWeeklyDensityResponse> buildWeeklyDensity(List<Schedule> schedules) {
        Map<DayOfWeek, DayDensity> byDay = new EnumMap<>(DayOfWeek.class);

        for (Schedule schedule : schedules) {
            if (schedule.getTimeSlot() == null || schedule.getTimeSlot().getDayOfWeek() == null
                    || schedule.getTimeSlot().getStartTime() == null || schedule.getTimeSlot().getEndTime() == null) {
                continue;
            }

            DayOfWeek day = schedule.getTimeSlot().getDayOfWeek();
            DayDensity density = byDay.computeIfAbsent(day, ignored -> new DayDensity());
            density.classCount += 1;
            density.totalMinutes += Math.max(0, Duration.between(
                    schedule.getTimeSlot().getStartTime(),
                    schedule.getTimeSlot().getEndTime()).toMinutes());
        }

        List<InstructorWeeklyDensityResponse> rows = new ArrayList<>();
        for (DayOfWeek day : ORDERED_DAYS) {
            DayDensity density = byDay.getOrDefault(day, DayDensity.EMPTY);
            if (day.getValue() > DayOfWeek.FRIDAY.getValue() && density.classCount == 0) {
                continue;
            }

            rows.add(new InstructorWeeklyDensityResponse(
                    day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    density.classCount,
                    density.totalMinutes));
        }

        return rows;
    }

    private InstructorFrictionSummaryResponse summarizeFriction(List<InstructorFrictionIssueResponse> frictions) {
        int high = 0;
        int medium = 0;
        int low = 0;

        for (InstructorFrictionIssueResponse issue : frictions) {
            if (issue.severity() == InstructorFrictionSeverity.HIGH) {
                high += 1;
            } else if (issue.severity() == InstructorFrictionSeverity.MEDIUM) {
                medium += 1;
            } else {
                low += 1;
            }
        }

        return new InstructorFrictionSummaryResponse(frictions.size(), high, medium, low);
    }

    private List<InstructorFrictionIssueResponse> computeFrictions(
            InstructorPreferenceSettings settings,
            List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            return List.of();
        }

        Map<DayOfWeek, List<Schedule>> byDay = groupByDaySorted(schedules);
        List<InstructorFrictionIssueResponse> issues = new ArrayList<>();
        int nextId = 1;

        for (Schedule schedule : schedules) {
            List<Schedule> daySchedules = byDay.get(schedule.getTimeSlot().getDayOfWeek());
            NeighborContext neighbors = neighborContext(daySchedules, schedule);

            InstructorFrictionIssueResponse largeGapIssue = buildLargeGapIssue(schedule, neighbors, settings, nextId);
            if (largeGapIssue != null) {
                issues.add(largeGapIssue);
                nextId++;
            }

            InstructorFrictionIssueResponse tightHopIssue = buildTightBuildingHopIssue(schedule, neighbors, settings, nextId);
            if (tightHopIssue != null) {
                issues.add(tightHopIssue);
                nextId++;
            }

            InstructorFrictionIssueResponse windowIssue = buildPreferredWindowIssue(schedule, settings, nextId);
            if (windowIssue != null) {
                issues.add(windowIssue);
                nextId++;
            }

            InstructorFrictionIssueResponse featureIssue = buildRoomFeatureIssue(schedule, settings, nextId);
            if (featureIssue != null) {
                issues.add(featureIssue);
                nextId++;
            }

            InstructorFrictionIssueResponse preferredBuildingIssue = buildPreferredBuildingIssue(schedule, settings, nextId);
            if (preferredBuildingIssue != null) {
                issues.add(preferredBuildingIssue);
                nextId++;
            }
        }

        issues.sort(
                Comparator.comparing((InstructorFrictionIssueResponse item) -> severityRank(item.severity())).reversed()
                        .thenComparing(InstructorFrictionIssueResponse::scheduleId)
                        .thenComparing(InstructorFrictionIssueResponse::id));

        return issues;
    }

    private static Map<DayOfWeek, List<Schedule>> groupByDaySorted(List<Schedule> schedules) {
        Map<DayOfWeek, List<Schedule>> byDay = new EnumMap<>(DayOfWeek.class);
        for (Schedule schedule : schedules) {
            DayOfWeek day = schedule.getTimeSlot().getDayOfWeek();
            byDay.computeIfAbsent(day, ignored -> new ArrayList<>()).add(schedule);
        }

        for (List<Schedule> daySchedules : byDay.values()) {
            daySchedules.sort(Comparator.comparing(item -> item.getTimeSlot().getStartTime()));
        }

        return byDay;
    }

    private NeighborContext neighborContext(List<Schedule> daySchedules, Schedule target) {
        if (daySchedules == null || daySchedules.isEmpty()) {
            return new NeighborContext(null, null, 0, 0);
        }

        Schedule previous = null;
        Schedule next = null;

        for (Schedule current : daySchedules) {
            if (current.getId().equals(target.getId())) {
                continue;
            }

            if (current.getTimeSlot().getEndTime().isBefore(target.getTimeSlot().getStartTime())
                    || current.getTimeSlot().getEndTime().equals(target.getTimeSlot().getStartTime())) {
                if (previous == null
                        || current.getTimeSlot().getEndTime().isAfter(previous.getTimeSlot().getEndTime())) {
                    previous = current;
                }
            }

            if (current.getTimeSlot().getStartTime().isAfter(target.getTimeSlot().getEndTime())
                    || current.getTimeSlot().getStartTime().equals(target.getTimeSlot().getEndTime())) {
                if (next == null
                        || current.getTimeSlot().getStartTime().isBefore(next.getTimeSlot().getStartTime())) {
                    next = current;
                }
            }
        }

        long gapBefore = previous == null
                ? 0
                : minutesBetween(previous.getTimeSlot().getEndTime(), target.getTimeSlot().getStartTime());

        long gapAfter = next == null
                ? 0
                : minutesBetween(target.getTimeSlot().getEndTime(), next.getTimeSlot().getStartTime());

        return new NeighborContext(previous, next, Math.max(0, gapBefore), Math.max(0, gapAfter));
    }

    private InstructorFrictionIssueResponse buildLargeGapIssue(
            Schedule schedule,
            NeighborContext neighbors,
            InstructorPreferenceSettings settings,
            int idSequence) {
        long maxGap = settings.maxGapMinutes();
        long overBefore = Math.max(0, neighbors.gapBeforeMinutes() - maxGap);
        long overAfter = Math.max(0, neighbors.gapAfterMinutes() - maxGap);

        if (overBefore == 0 && overAfter == 0) {
            return null;
        }

        boolean beforeIsWorse = overBefore >= overAfter;
        long chosenGap = beforeIsWorse ? neighbors.gapBeforeMinutes() : neighbors.gapAfterMinutes();
        long overBy = Math.max(overBefore, overAfter);

        InstructorFrictionSeverity severity = overBy >= 60
                ? InstructorFrictionSeverity.HIGH
                : (overBy >= 20 ? InstructorFrictionSeverity.MEDIUM : InstructorFrictionSeverity.LOW);

        String side = beforeIsWorse ? "before" : "after";
        String message = "Gap of " + chosenGap + " minutes " + side
                + " this class exceeds your max gap preference (" + maxGap + ")";

        return new InstructorFrictionIssueResponse(
                "friction-" + idSequence,
                InstructorFrictionType.LARGE_GAP,
                severity,
                schedule.getId(),
                message,
                beforeIsWorse ? RecommendedIssue.GAP_TOO_LARGE_BEFORE : RecommendedIssue.GAP_TOO_LARGE_AFTER);
    }

    private InstructorFrictionIssueResponse buildTightBuildingHopIssue(
            Schedule schedule,
            NeighborContext neighbors,
            InstructorPreferenceSettings settings,
            int idSequence) {
        if (!settings.avoidBuildingHops()) {
            return null;
        }

        Integer threshold = settings.minTravelBufferMinutes();
        if (threshold == null) {
            return null;
        }

        TightHop tightHop = tightHop(schedule, neighbors.previous(), threshold);
        TightHop nextHop = tightHop(schedule, neighbors.next(), threshold);
        if (tightHop == null || (nextHop != null && nextHop.gapMinutes() < tightHop.gapMinutes())) {
            tightHop = nextHop;
        }

        if (tightHop == null) {
            return null;
        }

        long deficit = Math.max(0, threshold - tightHop.gapMinutes());
        InstructorFrictionSeverity severity = deficit >= 10
                ? InstructorFrictionSeverity.HIGH
                : InstructorFrictionSeverity.MEDIUM;

        String message = "Back-to-back travel gap is only " + tightHop.gapMinutes() + " minutes between "
                + tightHop.fromBuilding() + " and " + tightHop.toBuilding() + " (preference: " + threshold + ")";

        return new InstructorFrictionIssueResponse(
                "friction-" + idSequence,
                InstructorFrictionType.TIGHT_BUILDING_HOP,
                severity,
                schedule.getId(),
                message,
                RecommendedIssue.BACK_TO_BACK_TRAVEL);
    }

    private TightHop tightHop(Schedule schedule, Schedule other, int threshold) {
        if (other == null) {
            return null;
        }

        Long targetBuildingId = schedule.getRoom() != null ? schedule.getRoom().getBuildingId() : null;
        Long otherBuildingId = other.getRoom() != null ? other.getRoom().getBuildingId() : null;
        if (Objects.equals(targetBuildingId, otherBuildingId)) {
            return null;
        }

        long gap;
        if (other.getTimeSlot().getEndTime().isBefore(schedule.getTimeSlot().getStartTime())
                || other.getTimeSlot().getEndTime().equals(schedule.getTimeSlot().getStartTime())) {
            gap = minutesBetween(other.getTimeSlot().getEndTime(), schedule.getTimeSlot().getStartTime());
        } else if (schedule.getTimeSlot().getEndTime().isBefore(other.getTimeSlot().getStartTime())
                || schedule.getTimeSlot().getEndTime().equals(other.getTimeSlot().getStartTime())) {
            gap = minutesBetween(schedule.getTimeSlot().getEndTime(), other.getTimeSlot().getStartTime());
        } else {
            return null;
        }

        if (gap >= threshold) {
            return null;
        }

        String from = other.getRoom() != null && other.getRoom().getBuildingCode() != null
                ? other.getRoom().getBuildingCode()
                : "another building";
        String to = schedule.getRoom() != null && schedule.getRoom().getBuildingCode() != null
                ? schedule.getRoom().getBuildingCode()
                : "another building";

        return new TightHop(gap, from, to);
    }

    private InstructorFrictionIssueResponse buildPreferredWindowIssue(
            Schedule schedule,
            InstructorPreferenceSettings settings,
            int idSequence) {
        if (settings.preferredStartTime() == null || settings.preferredEndTime() == null) {
            return null;
        }

        LocalTime start = schedule.getTimeSlot().getStartTime();
        LocalTime end = schedule.getTimeSlot().getEndTime();
        if (!start.isBefore(settings.preferredStartTime()) && !end.isAfter(settings.preferredEndTime())) {
            return null;
        }

        long earlyBy = start.isBefore(settings.preferredStartTime())
                ? minutesBetween(start, settings.preferredStartTime())
                : 0;
        long lateBy = end.isAfter(settings.preferredEndTime())
                ? minutesBetween(settings.preferredEndTime(), end)
                : 0;
        long deviation = Math.max(earlyBy, lateBy);

        InstructorFrictionSeverity severity = deviation >= 60
                ? InstructorFrictionSeverity.HIGH
                : (deviation >= 30 ? InstructorFrictionSeverity.MEDIUM : InstructorFrictionSeverity.LOW);

        String message = "Class falls outside your preferred time window ("
                + settings.preferredStartTime() + "-" + settings.preferredEndTime() + ")";

        return new InstructorFrictionIssueResponse(
                "friction-" + idSequence,
                InstructorFrictionType.OUTSIDE_PREFERRED_WINDOW,
                severity,
                schedule.getId(),
                message,
                RecommendedIssue.TIME_OF_DAY_PREFERENCE);
    }

    private InstructorFrictionIssueResponse buildRoomFeatureIssue(
            Schedule schedule,
            InstructorPreferenceSettings settings,
            int idSequence) {
        if (settings.requiredRoomFeatures().isEmpty()) {
            return null;
        }

        String roomFeatures = schedule.getRoom() != null ? schedule.getRoom().getFeatures() : null;

        List<String> missing = settings.requiredRoomFeatures().stream()
                .filter(feature -> !RoomFeatureCatalog.matchesRoomFeatures(roomFeatures, feature))
                .map(RoomFeatureCatalog::labelFor)
                .toList();

        if (missing.isEmpty()) {
            return null;
        }

        String message = "Room may be missing required features: " + String.join(", ", missing);
        return new InstructorFrictionIssueResponse(
                "friction-" + idSequence,
                InstructorFrictionType.ROOM_FEATURE_MISMATCH,
                InstructorFrictionSeverity.MEDIUM,
                schedule.getId(),
                message,
                RecommendedIssue.ROOM_EQUIPMENT_MISMATCH);
    }

    private InstructorFrictionIssueResponse buildPreferredBuildingIssue(
            Schedule schedule,
            InstructorPreferenceSettings settings,
            int idSequence) {
        if (settings.preferredBuildingIds().isEmpty()) {
            return null;
        }

        Long buildingId = schedule.getRoom() != null ? schedule.getRoom().getBuildingId() : null;
        if (buildingId != null && settings.preferredBuildingIds().contains(buildingId)) {
            return null;
        }

        String buildingCode = schedule.getRoom() != null ? schedule.getRoom().getBuildingCode() : null;
        String suffix = buildingCode == null ? "" : ": " + buildingCode;
        String message = "Class is outside your preferred buildings" + suffix;

        return new InstructorFrictionIssueResponse(
                "friction-" + idSequence,
                InstructorFrictionType.NON_PREFERRED_BUILDING,
                InstructorFrictionSeverity.LOW,
                schedule.getId(),
                message,
                RecommendedIssue.OTHER);
    }

    private boolean matchesQueueFilter(InstructorQueueRowResponse row, QueueFilter filter) {
        return switch (filter) {
            case COVERAGE_RISK -> row.coverageRiskLevel() != InstructorCoverageRiskLevel.LOW;
            case OVERLOADED -> row.loadStatus() == InstructorLoadStatus.OVER;
            case UNDER_UTILIZED -> row.loadStatus() == InstructorLoadStatus.UNDER;
            case PREFERENCE_INCOMPLETE -> row.preferenceCompletenessPercent() < PREFERENCE_COMPLETENESS_THRESHOLD;
            case FRICTION_HOTSPOTS -> isFrictionHotspot(row);
            case READY -> row.status() == InstructorOperationalStatus.READY;
            case ALL -> true;
        };
    }

    private Comparator<InstructorQueueRowResponse> queueComparator() {
        return Comparator
                .comparing((InstructorQueueRowResponse row) -> coverageRiskRank(row.coverageRiskLevel()))
                .reversed()
                .thenComparing(InstructorQueueRowResponse::overloadCredits, Comparator.reverseOrder())
                .thenComparing((InstructorQueueRowResponse row) -> severityRank(row.frictionSeverity()), Comparator.reverseOrder())
                .thenComparing(InstructorQueueRowResponse::frictionScore, Comparator.reverseOrder())
                .thenComparing(InstructorQueueRowResponse::lastName)
                .thenComparing(InstructorQueueRowResponse::firstName);
    }

    private QueueFilter parseQueueFilter(String rawFilter) {
        if (rawFilter == null || rawFilter.isBlank()) {
            return QueueFilter.ALL;
        }

        String normalized = rawFilter.trim().toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');

        return switch (normalized) {
            case "coverage-risk" -> QueueFilter.COVERAGE_RISK;
            case "overloaded" -> QueueFilter.OVERLOADED;
            case "under-utilized", "underutilized" -> QueueFilter.UNDER_UTILIZED;
            case "preference-incomplete" -> QueueFilter.PREFERENCE_INCOMPLETE;
            case "friction-hotspots", "friction-hotspot" -> QueueFilter.FRICTION_HOTSPOTS;
            case "ready" -> QueueFilter.READY;
            default -> QueueFilter.ALL;
        };
    }

    private String normalizeDepartmentFilter(String rawDepartment) {
        if (rawDepartment == null) {
            return null;
        }

        String trimmed = rawDepartment.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("all")) {
            return null;
        }

        return normalizeDepartment(trimmed);
    }

    private InstructorLoadStatus determineLoadStatus(int assignedCredits) {
        if (assignedCredits < TARGET_CREDITS_MIN) {
            return InstructorLoadStatus.UNDER;
        }
        if (assignedCredits > TARGET_CREDITS_MAX) {
            return InstructorLoadStatus.OVER;
        }
        return InstructorLoadStatus.BALANCED;
    }

    private InstructorCoverageRiskLevel determineDepartmentCoverageRisk(int unfilledCourseCount, int unfilledCredits) {
        if (unfilledCourseCount >= 4 || unfilledCredits >= 12) {
            return InstructorCoverageRiskLevel.HIGH;
        }
        if (unfilledCourseCount > 0 || unfilledCredits > 0) {
            return InstructorCoverageRiskLevel.MEDIUM;
        }
        return InstructorCoverageRiskLevel.LOW;
    }

    private InstructorCoverageRiskLevel determineInstructorCoverageRisk(
            InstructorCoverageRiskLevel departmentRisk,
            InstructorLoadStatus loadStatus) {
        if (departmentRisk == InstructorCoverageRiskLevel.LOW) {
            return InstructorCoverageRiskLevel.LOW;
        }

        if (departmentRisk == InstructorCoverageRiskLevel.HIGH
                || loadStatus == InstructorLoadStatus.OVER
                || loadStatus == InstructorLoadStatus.UNDER) {
            return InstructorCoverageRiskLevel.HIGH;
        }

        return InstructorCoverageRiskLevel.MEDIUM;
    }

    private InstructorOperationalStatus determineOperationalStatus(
            InstructorCoverageRiskLevel coverageRiskLevel,
            InstructorLoadStatus loadStatus,
            int preferenceCompleteness,
            InstructorFrictionSeverity frictionSeverity,
            int frictionScore) {
        if (coverageRiskLevel != InstructorCoverageRiskLevel.LOW) {
            return InstructorOperationalStatus.COVERAGE_RISK;
        }
        if (loadStatus == InstructorLoadStatus.OVER) {
            return InstructorOperationalStatus.OVERLOADED;
        }
        if (isFrictionHotspot(frictionSeverity, frictionScore)) {
            return InstructorOperationalStatus.FRICTION_HOTSPOT;
        }
        if (preferenceCompleteness < PREFERENCE_COMPLETENESS_THRESHOLD) {
            return InstructorOperationalStatus.PREFERENCE_INCOMPLETE;
        }
        if (loadStatus == InstructorLoadStatus.UNDER) {
            return InstructorOperationalStatus.UNDER_UTILIZED;
        }
        return InstructorOperationalStatus.READY;
    }

    private List<String> buildRecommendedActions(
            int assignedCourseCount,
            InstructorLoadStatus loadStatus,
            int preferenceCompleteness,
            List<InstructorFrictionIssueResponse> frictions,
            InstructorCoverageRiskLevel coverageRiskLevel,
            InstructorOperationalStatus status) {
        Set<String> actions = new LinkedHashSet<>();

        if (coverageRiskLevel != InstructorCoverageRiskLevel.LOW || assignedCourseCount == 0) {
            actions.add("Assign to open course(s)");
        }
        if (loadStatus == InstructorLoadStatus.OVER || loadStatus == InstructorLoadStatus.UNDER) {
            actions.add("Rebalance teaching load");
        }
        if (preferenceCompleteness < PREFERENCE_COMPLETENESS_THRESHOLD) {
            actions.add("Request preference update");
        }
        if (!frictions.isEmpty()) {
            actions.add("Review friction issues");
        }

        if (actions.isEmpty() || status == InstructorOperationalStatus.READY) {
            actions.add("Ready for solver run");
        }

        return List.copyOf(actions);
    }

    private boolean isFrictionHotspot(InstructorQueueRowResponse row) {
        return isFrictionHotspot(row.frictionSeverity(), row.frictionScore());
    }

    private boolean isFrictionHotspot(InstructorFrictionSeverity frictionSeverity, int frictionScore) {
        return frictionScore >= FRICTION_HOTSPOT_SCORE || frictionSeverity == InstructorFrictionSeverity.HIGH;
    }

    private InstructorFrictionSeverity determineFrictionSeverity(
            List<InstructorFrictionIssueResponse> frictions,
            int frictionScore) {
        long highCount = frictions.stream().filter(issue -> issue.severity() == InstructorFrictionSeverity.HIGH).count();
        long mediumCount = frictions.stream().filter(issue -> issue.severity() == InstructorFrictionSeverity.MEDIUM).count();

        if (highCount > 0 || frictionScore >= 10) {
            return InstructorFrictionSeverity.HIGH;
        }
        if (mediumCount > 0 || frictionScore >= 4) {
            return InstructorFrictionSeverity.MEDIUM;
        }
        return InstructorFrictionSeverity.LOW;
    }

    private int computeFrictionScore(List<InstructorFrictionIssueResponse> frictions) {
        int score = 0;
        for (InstructorFrictionIssueResponse issue : frictions) {
            score += switch (issue.severity()) {
                case HIGH -> 5;
                case MEDIUM -> 3;
                case LOW -> 1;
            };
        }
        return score;
    }

    private int computePreferenceCompleteness(InstructorPreference preference) {
        if (preference == null) {
            return 0;
        }

        int completed = 0;
        int total = 6;

        if (preference.getPreferredStartTime() != null) {
            completed += 1;
        }
        if (preference.getPreferredEndTime() != null) {
            completed += 1;
        }
        if (preference.getMaxGapMinutes() != null && preference.getMaxGapMinutes() > 0) {
            completed += 1;
        }
        if (preference.getMinTravelBufferMinutes() != null && preference.getMinTravelBufferMinutes() > 0) {
            completed += 1;
        }
        if (preference.getPreferredBuildingIds() != null && !preference.getPreferredBuildingIds().isEmpty()) {
            completed += 1;
        }
        if (preference.getRequiredRoomFeatures() != null && !preference.getRequiredRoomFeatures().isEmpty()) {
            completed += 1;
        }

        return Math.round((completed * 100.0f) / total);
    }

    private InstructorPreferenceSettings toSettings(InstructorPreference preference) {
        if (preference == null) {
            return defaultSettings();
        }

        List<Long> preferredBuildingIds = preference.getPreferredBuildingIds() == null
                ? List.of()
                : preference.getPreferredBuildingIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        List<String> requiredRoomFeatures = preference.getRequiredRoomFeatures() == null
                ? List.of()
                : preference.getRequiredRoomFeatures().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .distinct()
                        .toList();

        return new InstructorPreferenceSettings(
                preference.getPreferredStartTime() != null
                        ? preference.getPreferredStartTime()
                        : InstructorPreferenceService.DEFAULT_PREFERRED_START,
                preference.getPreferredEndTime() != null
                        ? preference.getPreferredEndTime()
                        : InstructorPreferenceService.DEFAULT_PREFERRED_END,
                preference.getMaxGapMinutes() != null
                        ? preference.getMaxGapMinutes()
                        : InstructorPreferenceService.DEFAULT_MAX_GAP_MINUTES,
                preference.getMinTravelBufferMinutes() != null
                        ? preference.getMinTravelBufferMinutes()
                        : InstructorPreferenceService.DEFAULT_MIN_TRAVEL_BUFFER_MINUTES,
                preference.isAvoidBuildingHops(),
                preferredBuildingIds,
                requiredRoomFeatures);
    }

    private InstructorPreferenceSettings defaultSettings() {
        return new InstructorPreferenceSettings(
                InstructorPreferenceService.DEFAULT_PREFERRED_START,
                InstructorPreferenceService.DEFAULT_PREFERRED_END,
                InstructorPreferenceService.DEFAULT_MAX_GAP_MINUTES,
                InstructorPreferenceService.DEFAULT_MIN_TRAVEL_BUFFER_MINUTES,
                InstructorPreferenceService.DEFAULT_AVOID_BUILDING_HOPS,
                List.of(),
                List.of());
    }

    private Long extractInstructorId(Schedule schedule) {
        if (schedule.getCourse() == null || schedule.getCourse().getInstructor() == null) {
            return null;
        }
        return schedule.getCourse().getInstructor().getId();
    }

    private String roomLabel(Schedule schedule) {
        if (schedule.getRoom() == null) {
            return null;
        }

        String buildingCode = schedule.getRoom().getBuildingCode() == null
                ? ""
                : schedule.getRoom().getBuildingCode().trim();
        String roomNumber = schedule.getRoom().getRoomNumber() == null
                ? ""
                : schedule.getRoom().getRoomNumber().trim();

        String joined = (buildingCode + " " + roomNumber).trim();
        return joined.isEmpty() ? "Room TBD" : joined;
    }

    private String normalizeDepartment(String rawDepartment) {
        if (rawDepartment == null || rawDepartment.trim().isEmpty()) {
            return "Undeclared";
        }
        return rawDepartment.trim();
    }

    private int safeCredits(Course course) {
        return course.getCredits() == null ? 0 : course.getCredits();
    }

    private int safeEnrollmentCapacity(Course course) {
        return course.getEnrollmentCapacity() == null ? 0 : course.getEnrollmentCapacity();
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static long minutesBetween(LocalTime from, LocalTime to) {
        return Duration.between(from, to).toMinutes();
    }

    private static int severityRank(InstructorFrictionSeverity severity) {
        return switch (severity) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private static int coverageRiskRank(InstructorCoverageRiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private enum QueueFilter {
        ALL,
        COVERAGE_RISK,
        OVERLOADED,
        UNDER_UTILIZED,
        PREFERENCE_INCOMPLETE,
        FRICTION_HOTSPOTS,
        READY
    }

    private record InsightsSnapshot(
            List<InstructorQueueRowResponse> queueRows,
            List<InstructorDepartmentLoadResponse> departmentLoads,
            Map<Long, List<InstructorFrictionIssueResponse>> frictionByInstructor) {
    }

    private static final class DepartmentDemand {
        static final DepartmentDemand EMPTY = new DepartmentDemand();

        int unfilledCourseCount;
        int unfilledCredits;
    }

    private static final class DayDensity {
        static final DayDensity EMPTY = new DayDensity();

        int classCount;
        long totalMinutes;
    }

    private record NeighborContext(Schedule previous, Schedule next, long gapBeforeMinutes, long gapAfterMinutes) {
    }

    private record TightHop(long gapMinutes, String fromBuilding, String toBuilding) {
    }

    private record TimedChange(LocalDateTime timestamp, String label, String source) {
    }
}
