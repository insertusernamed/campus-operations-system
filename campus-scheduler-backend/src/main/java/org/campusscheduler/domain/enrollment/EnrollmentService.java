package org.campusscheduler.domain.enrollment;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service layer for Enrollment read operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * Get all enrollments.
     *
     * @return list of enrollments
     */
    public List<Enrollment> findAll() {
        return initializeAndSort(enrollmentRepository.findAll());
    }

    /**
     * Find enrollments using the supported API filters.
     *
     * @param studentId the student filter
     * @param courseId the course filter
     * @param scheduleId the schedule filter
     * @param semester the semester filter
     * @return filtered enrollments
     */
    public List<Enrollment> findByFilters(Long studentId, Long courseId, Long scheduleId, String semester) {
        List<Enrollment> base = selectBaseQuery(studentId, courseId, scheduleId, semester);

        return initializeAndSort(base).stream()
                .filter(enrollment -> studentId == null
                        || (enrollment.getStudent() != null && studentId.equals(enrollment.getStudent().getId())))
                .filter(enrollment -> courseId == null
                        || (enrollment.getCourse() != null && courseId.equals(enrollment.getCourse().getId())))
                .filter(enrollment -> scheduleId == null
                        || (enrollment.getSchedule() != null && scheduleId.equals(enrollment.getSchedule().getId())))
                .filter(enrollment -> semester == null || semester.isBlank() || semester.equals(enrollment.getSemester()))
                .toList();
    }

    /**
     * Find enrollments by student.
     *
     * @param studentId the student ID
     * @return list of enrollments
     */
    public List<Enrollment> findByStudent(Long studentId) {
        return initializeAndSort(enrollmentRepository.findByStudentId(studentId));
    }

    /**
     * Find enrollments by student and semester.
     *
     * @param studentId the student ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByStudentAndSemester(Long studentId, String semester) {
        return initializeAndSort(enrollmentRepository.findByStudentIdAndSemester(studentId, semester));
    }

    /**
     * Find enrollments by course and semester.
     *
     * @param courseId the course ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByCourseAndSemester(Long courseId, String semester) {
        return initializeAndSort(enrollmentRepository.findByCourseIdAndSemester(courseId, semester));
    }

    /**
     * Find enrollments by schedule and semester.
     *
     * @param scheduleId the schedule ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByScheduleAndSemester(Long scheduleId, String semester) {
        return initializeAndSort(enrollmentRepository.findByScheduleIdAndSemester(scheduleId, semester));
    }

    /**
     * Find enrollments by student, course, and semester.
     *
     * @param studentId the student ID
     * @param courseId the course ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByStudentCourseAndSemester(Long studentId, Long courseId, String semester) {
        return initializeAndSort(enrollmentRepository.findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester));
    }

    /**
     * Find enrollments by student, schedule, and semester.
     *
     * @param studentId the student ID
     * @param scheduleId the schedule ID
     * @param semester the semester name
     * @return list of enrollments
     */
    public List<Enrollment> findByStudentScheduleAndSemester(Long studentId, Long scheduleId, String semester) {
        return initializeAndSort(enrollmentRepository.findByStudentIdAndScheduleIdAndSemester(studentId, scheduleId, semester));
    }

    private List<Enrollment> selectBaseQuery(Long studentId, Long courseId, Long scheduleId, String semester) {
        boolean hasSemester = semester != null && !semester.isBlank();

        if (studentId != null && courseId != null && hasSemester) {
            return enrollmentRepository.findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester);
        }
        if (studentId != null && scheduleId != null && hasSemester) {
            return enrollmentRepository.findByStudentIdAndScheduleIdAndSemester(studentId, scheduleId, semester);
        }
        if (studentId != null && hasSemester) {
            return enrollmentRepository.findByStudentIdAndSemester(studentId, semester);
        }
        if (courseId != null && hasSemester) {
            return enrollmentRepository.findByCourseIdAndSemester(courseId, semester);
        }
        if (scheduleId != null && hasSemester) {
            return enrollmentRepository.findByScheduleIdAndSemester(scheduleId, semester);
        }
        if (studentId != null) {
            return enrollmentRepository.findByStudentId(studentId);
        }
        if (courseId != null) {
            return enrollmentRepository.findByCourseId(courseId);
        }
        if (scheduleId != null) {
            return enrollmentRepository.findByScheduleId(scheduleId);
        }
        if (hasSemester) {
            return enrollmentRepository.findBySemester(semester);
        }
        return enrollmentRepository.findAll();
    }

    private List<Enrollment> initializeAndSort(List<Enrollment> enrollments) {
        return enrollments.stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .sorted(enrollmentComparator())
                .toList();
    }

    private void initialize(Enrollment enrollment) {
        if (enrollment.getStudent() != null) {
            enrollment.getStudent().getId();
            enrollment.getStudent().getStudentNumber();
            List<Long> preferredCourseIds = enrollment.getStudent().getPreferredCourseIds();
            if (preferredCourseIds != null) {
                preferredCourseIds.size();
            }
        }

        if (enrollment.getCourse() != null) {
            enrollment.getCourse().getId();
            enrollment.getCourse().getCode();
            if (enrollment.getCourse().getInstructor() != null) {
                enrollment.getCourse().getInstructor().getId();
                enrollment.getCourse().getInstructor().getFirstName();
            }
        }

        Schedule schedule = enrollment.getSchedule();
        if (schedule == null) {
            return;
        }

        schedule.getId();
        schedule.getSemester();
        if (schedule.getCourse() != null) {
            schedule.getCourse().getId();
            schedule.getCourse().getCode();
            if (schedule.getCourse().getInstructor() != null) {
                schedule.getCourse().getInstructor().getId();
                schedule.getCourse().getInstructor().getFirstName();
            }
        }
        if (schedule.getRoom() != null) {
            schedule.getRoom().getId();
            schedule.getRoom().getRoomNumber();
            schedule.getRoom().getFeatureSet().size();
            schedule.getRoom().getAccessibilityFlags().size();
        }

        TimeSlot timeSlot = schedule.getTimeSlot();
        if (timeSlot != null) {
            timeSlot.getId();
            timeSlot.getDayOfWeek();
            timeSlot.getStartTime();
            timeSlot.getEndTime();
        }
    }

    private Comparator<Enrollment> enrollmentComparator() {
        return Comparator
                .comparing((Enrollment enrollment) -> normalize(enrollment.getSemester()))
                .thenComparing(enrollment -> enrollment.getStudent() != null
                        ? normalize(enrollment.getStudent().getStudentNumber())
                        : "")
                .thenComparing(enrollment -> enrollment.getSchedule() != null
                        && enrollment.getSchedule().getCourse() != null
                                ? normalize(enrollment.getSchedule().getCourse().getCode())
                                : "")
                .thenComparing(enrollment -> enrollment.getSchedule() != null
                        && enrollment.getSchedule().getTimeSlot() != null
                        && enrollment.getSchedule().getTimeSlot().getDayOfWeek() != null
                                ? enrollment.getSchedule().getTimeSlot().getDayOfWeek().getValue()
                                : Integer.MAX_VALUE)
                .thenComparing(enrollment -> enrollment.getSchedule() != null
                        && enrollment.getSchedule().getTimeSlot() != null
                        && enrollment.getSchedule().getTimeSlot().getStartTime() != null
                                ? enrollment.getSchedule().getTimeSlot().getStartTime()
                                : java.time.LocalTime.MAX)
                .thenComparing(enrollment -> enrollment.getId() == null ? Long.MAX_VALUE : enrollment.getId());
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }
}
