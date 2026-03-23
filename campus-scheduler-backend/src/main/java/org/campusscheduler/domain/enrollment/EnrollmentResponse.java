package org.campusscheduler.domain.enrollment;

import org.campusscheduler.domain.schedule.ScheduleResponse;
import org.campusscheduler.domain.student.Student;

/**
 * API response model for enrollment and waitlist rows.
 */
public record EnrollmentResponse(
        Long id,
        String semester,
        EnrollmentStatus status,
        StudentSummary student,
        ScheduleResponse schedule) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getSemester(),
                enrollment.getStatus(),
                StudentSummary.from(enrollment.getStudent()),
                ScheduleResponse.from(enrollment.getSchedule()));
    }

    public record StudentSummary(
            Long id,
            String studentNumber,
            String firstName,
            String lastName,
            String email,
            String department,
            Integer yearLevel,
            Integer targetCourseLoad) {

        public static StudentSummary from(Student student) {
            if (student == null) {
                return null;
            }
            return new StudentSummary(
                    student.getId(),
                    student.getStudentNumber(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getEmail(),
                    student.getDepartment(),
                    student.getYearLevel(),
                    student.getTargetCourseLoad());
        }
    }
}
