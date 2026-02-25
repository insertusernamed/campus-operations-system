package org.campusscheduler.domain.instructorpreference;

import java.util.List;

/**
 * Canonical selectable room feature option for instructor preferences.
 */
public record RoomFeatureOptionResponse(
        String value,
        String label,
        String category,
        List<String> matchKeywords
) {
}
