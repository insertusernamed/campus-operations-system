package org.campusscheduler.domain.room;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * DTO for Room responses that includes building information.
 */
@Data
@Builder
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private Integer capacity;
    private Room.RoomType type;
    private Room.AvailabilityStatus availabilityStatus;
    private String features;
    private List<String> featureSet;
    private List<String> accessibilityFlags;
    private String operationalNotes;
    private LocalDate lastInspectionDate;
    private Long buildingId;
    private String buildingCode;
    private String buildingName;

    /**
     * Creates a RoomResponse from a Room entity.
     */
    public static RoomResponse fromEntity(Room room) {
        RoomResponseBuilder builder = RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .capacity(room.getCapacity())
                .type(room.getType())
                .availabilityStatus(room.getAvailabilityStatus())
                .features(room.getFeatures())
                .featureSet(toSortedFeatureList(resolveFeatureSet(room)))
                .accessibilityFlags(toSortedFeatureList(room.getAccessibilityFlags()))
                .operationalNotes(room.getOperationalNotes())
                .lastInspectionDate(room.getLastInspectionDate());

        if (room.getBuilding() != null) {
            builder.buildingId(room.getBuilding().getId())
                    .buildingCode(room.getBuilding().getCode())
                    .buildingName(room.getBuilding().getName());
        }

        return builder.build();
    }

    private static Set<String> resolveFeatureSet(Room room) {
        Set<String> stored = room.getFeatureSet();
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }

        if (room.getFeatures() == null || room.getFeatures().isBlank()) {
            return Set.of();
        }

        LinkedHashSet<String> fallback = new LinkedHashSet<>();
        for (String token : room.getFeatures().split(",")) {
            String normalized = normalizeTag(token);
            if (!normalized.isEmpty()) {
                fallback.add(normalized);
            }
        }
        return fallback;
    }

    private static List<String> toSortedFeatureList(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(RoomResponse::normalizeTag)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private static String normalizeTag(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
