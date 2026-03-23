package org.campusscheduler.domain.student;

import java.util.List;

/**
 * API response model for student payloads.
 */
public record StudentResponse(
        Long id,
        String studentNumber,
        String firstName,
        String lastName,
        String email,
        String department,
        Integer yearLevel,
        Integer targetCourseLoad,
        List<Long> preferredCourseIds) {

    public static StudentResponse from(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getDepartment(),
                student.getYearLevel(),
                student.getTargetCourseLoad(),
                student.getPreferredCourseIds() == null ? List.of() : List.copyOf(student.getPreferredCourseIds()));
    }
}
