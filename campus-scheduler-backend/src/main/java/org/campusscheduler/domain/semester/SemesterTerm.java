package org.campusscheduler.domain.semester;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canonical semester term definitions.
 *
 * The semester label year is used as the anchor year and offsets are applied to
 * get real calendar dates (for winter, start is in the previous year).
 */
@Getter
@RequiredArgsConstructor
public enum SemesterTerm {

    WINTER("Winter", 12, 21, 3, 20, -1, 0),
    SPRING("Spring", 3, 21, 6, 20, 0, 0),
    SUMMER("Summer", 6, 21, 9, 20, 0, 0),
    FALL("Fall", 9, 21, 12, 20, 0, 0);

    private final String displayName;
    private final int startMonth;
    private final int startDay;
    private final int endMonth;
    private final int endDay;
    private final int startYearOffset;
    private final int endYearOffset;
}
