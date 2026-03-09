package org.campusscheduler.domain.instructorinsight;

/**
 * Instructor assignment row with scheduling context.
 */
public record InstructorAssignedCourseContextResponse(
        Long courseId,
        String code,
        String name,
        int credits,
        int enrollmentCapacity,
        boolean scheduled,
        String dayOfWeek,
        String startTime,
        String endTime,
        String roomLabel,
        String semester
) {
}
