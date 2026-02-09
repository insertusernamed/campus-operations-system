package org.campusscheduler.domain.semester;

/**
 * Semester definition payload shared with the frontend.
 */
public record SemesterDefinitionDTO(
        SemesterTerm term,
        String displayName,
        int startMonth,
        int startDay,
        int endMonth,
        int endDay,
        int startYearOffset,
        int endYearOffset
) {
}

