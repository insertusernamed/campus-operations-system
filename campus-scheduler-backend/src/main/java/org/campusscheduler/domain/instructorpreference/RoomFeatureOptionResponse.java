package org.campusscheduler.domain.instructorpreference;

/**
 * Canonical selectable room feature option for instructor preferences.
 */
public record RoomFeatureOptionResponse(
        String value,
        String label,
        String category
) {
}
