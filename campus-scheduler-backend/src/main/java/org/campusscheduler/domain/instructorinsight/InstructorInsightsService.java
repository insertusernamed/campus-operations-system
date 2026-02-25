package org.campusscheduler.domain.instructorinsight;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceSettings;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceService;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Computes proactive friction issues for an instructor in a semester.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorInsightsService {

    private final ScheduleRepository scheduleRepository;
    private final InstructorPreferenceService instructorPreferenceService;

    public Optional<List<InstructorFrictionIssueResponse>> findFrictions(Long instructorId, String semester) {
        Optional<InstructorPreferenceSettings> settingsOpt = instructorPreferenceService.getEffectiveSettings(instructorId);
        if (settingsOpt.isEmpty()) {
            return Optional.empty();
        }

        InstructorPreferenceSettings settings = settingsOpt.get();
        List<Schedule> schedules = scheduleRepository.findByCourseInstructorIdAndSemester(instructorId, semester);
        if (schedules.isEmpty()) {
            return Optional.of(List.of());
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

        return Optional.of(issues);
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

        String roomFeatures = schedule.getRoom() != null && schedule.getRoom().getFeatures() != null
                ? schedule.getRoom().getFeatures().toLowerCase()
                : "";

        List<String> missing = settings.requiredRoomFeatures().stream()
                .map(String::toLowerCase)
                .filter(feature -> !roomFeatures.contains(feature))
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

    private static long minutesBetween(LocalTime from, LocalTime to) {
        return Duration.between(from, to).toMinutes();
    }

    private static int severityRank(InstructorFrictionSeverity severity) {
        Map<InstructorFrictionSeverity, Integer> ranks = new HashMap<>();
        ranks.put(InstructorFrictionSeverity.LOW, 1);
        ranks.put(InstructorFrictionSeverity.MEDIUM, 2);
        ranks.put(InstructorFrictionSeverity.HIGH, 3);
        return ranks.getOrDefault(severity, 0);
    }

    private record NeighborContext(Schedule previous, Schedule next, long gapBeforeMinutes, long gapAfterMinutes) {
    }

    private record TightHop(long gapMinutes, String fromBuilding, String toBuilding) {
    }
}
