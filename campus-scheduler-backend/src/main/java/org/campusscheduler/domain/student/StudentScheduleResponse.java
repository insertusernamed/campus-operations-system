package org.campusscheduler.domain.student;

import org.campusscheduler.domain.enrollment.EnrollmentResponse;

import java.util.List;

/**
 * API response model for a student's semester schedule, split by roster status.
 */
public record StudentScheduleResponse(
        Long studentId,
        String semester,
        List<EnrollmentResponse> enrolled,
        List<EnrollmentResponse> waitlisted) {
}
