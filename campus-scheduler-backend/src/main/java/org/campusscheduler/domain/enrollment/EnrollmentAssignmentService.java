package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.schedule.Schedule;
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

        Map<Long, ScheduledOffering> offeringsByCourseId = indexSchedulesByCourseId(schedules, semester);
        if (offeringsByCourseId.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> enrolledCountsByCourseId = new LinkedHashMap<>();

        return students.stream()
                .filter(Objects::nonNull)
                .sorted(studentComparator())
                .map(student -> assignStudent(student, offeringsByCourseId, enrolledCountsByCourseId, semester))
                .flatMap(List::stream)
                .toList();
    }

    private Map<Long, ScheduledOffering> indexSchedulesByCourseId(Collection<Schedule> schedules, String semester) {
        return schedules.stream()
                .filter(Objects::nonNull)
                .filter(schedule -> semester.equals(schedule.getSemester()))
                .filter(schedule -> schedule.getCourse() != null && schedule.getCourse().getId() != null)
                .sorted(Comparator
                        .comparing((Schedule schedule) -> schedule.getCourse().getId())
                        .thenComparing(schedule -> schedule.getId() == null ? Long.MAX_VALUE : schedule.getId()))
                .collect(
                        LinkedHashMap::new,
                        (indexed, schedule) -> indexed.putIfAbsent(
                                schedule.getCourse().getId(),
                                new ScheduledOffering(schedule, resolveSeatLimit(schedule))),
                        LinkedHashMap::putAll);
    }

    private List<Enrollment> assignStudent(
            Student student,
            Map<Long, ScheduledOffering> offeringsByCourseId,
            Map<Long, Integer> enrolledCountsByCourseId,
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

            ScheduledOffering offering = offeringsByCourseId.get(preferredCourseId);
            if (offering == null) {
                continue;
            }

            Schedule schedule = offering.schedule();
            if (!canEnroll(schedule, enrolledSchedules, dailyEnrolledCounts)) {
                continue;
            }

            EnrollmentStatus status = determineStatus(preferredCourseId, offering.seatLimit(), enrolledCountsByCourseId);

            assignments.add(Enrollment.builder()
                    .student(student)
                    .course(schedule.getCourse())
                    .schedule(schedule)
                    .semester(semester)
                    .status(status)
                    .build());

            if (status == EnrollmentStatus.ENROLLED) {
                enrolledCount++;
                enrolledSchedules.add(schedule);
                incrementDailyLoad(schedule, dailyEnrolledCounts);
            }

            if (enrolledCount >= targetCourseLoad) {
                break;
            }
        }

        return assignments;
    }

    private EnrollmentStatus determineStatus(
            Long courseId,
            int seatLimit,
            Map<Long, Integer> enrolledCountsByCourseId) {
        int currentCount = enrolledCountsByCourseId.getOrDefault(courseId, 0);
        if (currentCount < seatLimit) {
            enrolledCountsByCourseId.put(courseId, currentCount + 1);
            return EnrollmentStatus.ENROLLED;
        }
        return EnrollmentStatus.WAITLISTED;
    }

    private int resolveSeatLimit(Schedule schedule) {
        Integer courseCapacity = schedule.getCourse().getEnrollmentCapacity();
        Integer roomCapacity = schedule.getRoom() != null ? schedule.getRoom().getCapacity() : null;

        if (courseCapacity == null && roomCapacity == null) {
            return 0;
        }
        if (courseCapacity == null) {
            return roomCapacity;
        }
        if (roomCapacity == null) {
            return courseCapacity;
        }
        return Math.min(courseCapacity, roomCapacity);
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

    private record ScheduledOffering(Schedule schedule, int seatLimit) {
    }
}
