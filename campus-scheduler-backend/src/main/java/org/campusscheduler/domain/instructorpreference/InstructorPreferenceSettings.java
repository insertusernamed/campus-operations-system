package org.campusscheduler.domain.instructorpreference;

import java.time.LocalTime;
import java.util.List;

/**
 * Effective preferences used for friction analysis and ranking.
 */
public record InstructorPreferenceSettings(
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        int maxGapMinutes,
        int minTravelBufferMinutes,
        boolean avoidBuildingHops,
        List<Long> preferredBuildingIds,
        List<String> requiredRoomFeatures
) {
}
