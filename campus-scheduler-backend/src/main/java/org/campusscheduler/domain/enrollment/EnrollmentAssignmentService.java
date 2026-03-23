package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleSeatLimitResolver;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds an enrollment plan for generated student demand against a semester's
 * scheduled offerings.
 */
@Service
public class EnrollmentAssignmentService {

    static final int MAX_CLASSES_PER_DAY = 3;

    /**
     * Build unsaved enrollment rows for the provided semester by walking each
     * student's ranked demand in a deterministic order.
     *
     * @param students students with ranked course preferences
     * @param schedules scheduled classes that may satisfy those preferences
     * @param semester semester to assign
     * @return unsaved enrollment rows in deterministic student/preference order
     */
    public List<Enrollment> assignEnrollments(
            Collection<Student> students,
            Collection<Schedule> schedules,
            String semester) {
        if (students == null || students.isEmpty() || schedules == null || schedules.isEmpty() || semester == null) {
            return List.of();
        }

        Map<Long, List<ScheduledOffering>> offeringsByCourseId = indexSchedulesByCourseId(schedules, semester);
        if (offeringsByCourseId.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> enrolledCountsByOfferingId = new LinkedHashMap<>();

        return students.stream()
                .filter(Objects::nonNull)
                .sorted(studentComparator())
                .map(student -> assignStudent(student, offeringsByCourseId, enrolledCountsByOfferingId, semester))
                .flatMap(List::stream)
                .toList();
    }

    private Map<Long, List<ScheduledOffering>> indexSchedulesByCourseId(Collection<Schedule> schedules, String semester) {
        List<Schedule> sortedSchedules = schedules.stream()
                .filter(Objects::nonNull)
                .filter(schedule -> semester.equals(schedule.getSemester()))
                .filter(schedule -> schedule.getCourse() != null && schedule.getCourse().getId() != null)
                .sorted(Comparator
                        .comparing((Schedule schedule) -> schedule.getCourse().getId())
                        .thenComparing(schedule -> schedule.getId() == null ? Long.MAX_VALUE : schedule.getId()))
                .toList();

        Map<Long, List<ScheduledOffering>> indexed = new LinkedHashMap<>();
        long syntheticOfferingId = -1L;

        for (Schedule schedule : sortedSchedules) {
            long offeringId = schedule.getId() != null ? schedule.getId() : syntheticOfferingId--;
            indexed.computeIfAbsent(schedule.getCourse().getId(), ignored -> new ArrayList<>())
                    .add(new ScheduledOffering(offeringId, schedule, ScheduleSeatLimitResolver.resolve(schedule)));
        }

        return indexed;
    }

    private List<Enrollment> assignStudent(
            Student student,
            Map<Long, List<ScheduledOffering>> offeringsByCourseId,
            Map<Long, Integer> enrolledCountsByOfferingId,
            String semester) {
        List<Long> preferredCourseIds = student.getPreferredCourseIds();
        if (preferredCourseIds == null || preferredCourseIds.isEmpty()) {
            return List.of();
        }

        int targetCourseLoad = student.getTargetCourseLoad() == null ? preferredCourseIds.size() : student.getTargetCourseLoad();
        if (targetCourseLoad <= 0) {
            return List.of();
        }

        List<Enrollment> assignments = new ArrayList<>();
        Set<Long> assignedCourseIds = new LinkedHashSet<>();
        List<Schedule> enrolledSchedules = new ArrayList<>();
        Map<DayOfWeek, Integer> dailyEnrolledCounts = new EnumMap<>(DayOfWeek.class);
        int enrolledCount = 0;

        for (Long preferredCourseId : preferredCourseIds) {
            if (preferredCourseId == null || !assignedCourseIds.add(preferredCourseId)) {
                continue;
            }

            List<ScheduledOffering> offerings = offeringsByCourseId.get(preferredCourseId);
            if (offerings == null || offerings.isEmpty()) {
                continue;
            }

            ScheduledOffering enrolledOffering = findFirstEnrollableOffering(
                    offerings,
                    enrolledSchedules,
                    dailyEnrolledCounts,
                    enrolledCountsByOfferingId);
            if (enrolledOffering != null) {
                Schedule schedule = enrolledOffering.schedule();
                assignments.add(Enrollment.builder()
                        .student(student)
                        .course(schedule.getCourse())
                        .schedule(schedule)
                        .semester(semester)
                        .status(EnrollmentStatus.ENROLLED)
                        .build());

                reserveSeat(enrolledOffering.offeringId(), enrolledCountsByOfferingId);
                enrolledCount++;
                enrolledSchedules.add(schedule);
                incrementDailyLoad(schedule, dailyEnrolledCounts);

                if (enrolledCount >= targetCourseLoad) {
                    break;
                }
                continue;
            }

            ScheduledOffering waitlistOffering = offerings.stream()
                    .filter(offering -> canEnroll(offering.schedule(), enrolledSchedules, dailyEnrolledCounts))
                    .findFirst()
                    .orElse(null);
            if (waitlistOffering != null) {
                Schedule schedule = waitlistOffering.schedule();
                assignments.add(Enrollment.builder()
                        .student(student)
                        .course(schedule.getCourse())
                        .schedule(schedule)
                        .semester(semester)
                        .status(EnrollmentStatus.WAITLISTED)
                        .build());
            }
        }

        return assignments;
    }

    private ScheduledOffering findFirstEnrollableOffering(
            List<ScheduledOffering> offerings,
            List<Schedule> enrolledSchedules,
            Map<DayOfWeek, Integer> dailyEnrolledCounts,
            Map<Long, Integer> enrolledCountsByOfferingId) {
        for (ScheduledOffering offering : offerings) {
            if (!canEnroll(offering.schedule(), enrolledSchedules, dailyEnrolledCounts)) {
                continue;
            }
            if (hasAvailableSeat(offering, enrolledCountsByOfferingId)) {
                return offering;
            }
        }
        return null;
    }

    private boolean hasAvailableSeat(
            ScheduledOffering offering,
            Map<Long, Integer> enrolledCountsByOfferingId) {
        return enrolledCountsByOfferingId.getOrDefault(offering.offeringId(), 0) < offering.seatLimit();
    }

    private void reserveSeat(Long offeringId, Map<Long, Integer> enrolledCountsByOfferingId) {
        enrolledCountsByOfferingId.merge(offeringId, 1, Integer::sum);
    }

    private boolean canEnroll(
            Schedule schedule,
            List<Schedule> enrolledSchedules,
            Map<DayOfWeek, Integer> dailyEnrolledCounts) {
        TimeSlot candidateTimeSlot = schedule.getTimeSlot();
        if (candidateTimeSlot == null || candidateTimeSlot.getDayOfWeek() == null) {
            return true;
        }

        if (dailyEnrolledCounts.getOrDefault(candidateTimeSlot.getDayOfWeek(), 0) >= MAX_CLASSES_PER_DAY) {
            return false;
        }

        return enrolledSchedules.stream()
                .map(Schedule::getTimeSlot)
                .filter(Objects::nonNull)
                .noneMatch(existing -> existing.overlapsWith(candidateTimeSlot));
    }

    private void incrementDailyLoad(Schedule schedule, Map<DayOfWeek, Integer> dailyEnrolledCounts) {
        TimeSlot timeSlot = schedule.getTimeSlot();
        if (timeSlot == null || timeSlot.getDayOfWeek() == null) {
            return;
        }

        dailyEnrolledCounts.merge(timeSlot.getDayOfWeek(), 1, Integer::sum);
    }

    private Comparator<Student> studentComparator() {
        return Comparator
                .comparing((Student student) -> normalize(student.getStudentNumber()))
                .thenComparing(student -> normalize(student.getEmail()))
                .thenComparing(student -> student.getId() == null ? Long.MAX_VALUE : student.getId());
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private record ScheduledOffering(Long offeringId, Schedule schedule, int seatLimit) {
    }
}
