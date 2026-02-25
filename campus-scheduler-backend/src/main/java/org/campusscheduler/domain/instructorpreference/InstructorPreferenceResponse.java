package org.campusscheduler.domain.instructorpreference;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * API payload returned for instructor preferences.
 */
public record InstructorPreferenceResponse(
        Long instructorId,
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        Integer maxGapMinutes,
        Integer minTravelBufferMinutes,
        boolean avoidBuildingHops,
        List<Long> preferredBuildingIds,
        List<String> requiredRoomFeatures,
        LocalDateTime updatedAt
) {
}
