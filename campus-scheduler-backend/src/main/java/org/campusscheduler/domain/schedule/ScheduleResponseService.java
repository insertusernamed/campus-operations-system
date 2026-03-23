package org.campusscheduler.domain.schedule;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.enrollment.Enrollment;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.enrollment.EnrollmentStatus;
import org.campusscheduler.domain.schedule.ScheduleResponse.ScheduleSeatSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds API-ready schedule payloads with computed seat and waitlist data.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleResponseService {

    private final EnrollmentRepository enrollmentRepository;

    public List<ScheduleResponse> toResponses(Collection<Schedule> schedules) {
        List<Schedule> safeSchedules = schedules == null
                ? List.of()
                : schedules.stream().filter(Objects::nonNull).toList();
        Map<Long, ScheduleSeatSummary> summariesByScheduleId = buildSeatSummaries(safeSchedules);

        return safeSchedules.stream()
                .map(schedule -> ScheduleResponse.from(
                        schedule,
                        summariesByScheduleId.getOrDefault(schedule.getId(), buildDefaultSummary(schedule))))
                .toList();
    }

    public ScheduleResponse toResponse(Schedule schedule) {
        if (schedule == null) {
            return null;
        }
        return toResponses(List.of(schedule)).getFirst();
    }

    private Map<Long, ScheduleSeatSummary> buildSeatSummaries(List<Schedule> schedules) {
        List<Long> scheduleIds = schedules.stream()
                .map(Schedule::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (scheduleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, SeatCounts> countsByScheduleId = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollmentRepository.findByScheduleIdIn(scheduleIds)) {
            if (enrollment == null || enrollment.getSchedule() == null || enrollment.getSchedule().getId() == null) {
                continue;
            }

            SeatCounts counts = countsByScheduleId.computeIfAbsent(
                    enrollment.getSchedule().getId(),
                    ignored -> new SeatCounts());
            if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                counts.filledSeats++;
            } else if (enrollment.getStatus() == EnrollmentStatus.WAITLISTED) {
                counts.waitlistCount++;
            }
        }

        Map<Long, ScheduleSeatSummary> summaries = new LinkedHashMap<>();
        for (Schedule schedule : schedules) {
            if (schedule.getId() == null) {
                continue;
            }
            SeatCounts counts = countsByScheduleId.getOrDefault(schedule.getId(), new SeatCounts());
            summaries.put(schedule.getId(), buildSummary(schedule, counts.filledSeats, counts.waitlistCount));
        }
        return summaries;
    }

    private ScheduleSeatSummary buildDefaultSummary(Schedule schedule) {
        return buildSummary(schedule, 0, 0);
    }

    private ScheduleSeatSummary buildSummary(Schedule schedule, int filledSeats, int waitlistCount) {
        int seatLimit = ScheduleSeatLimitResolver.resolve(schedule);
        return new ScheduleSeatSummary(
                filledSeats,
                seatLimit,
                Math.max(seatLimit - filledSeats, 0),
                waitlistCount);
    }

    private static final class SeatCounts {
        private int filledSeats;
        private int waitlistCount;
    }
}
