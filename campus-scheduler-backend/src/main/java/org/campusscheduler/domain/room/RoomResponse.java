package org.campusscheduler.domain.room;

import lombok.Builder;
import lombok.Data;

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
    private String features;
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
                .features(room.getFeatures());

        if (room.getBuilding() != null) {
            builder.buildingId(room.getBuilding().getId())
                    .buildingCode(room.getBuilding().getCode())
                    .buildingName(room.getBuilding().getName());
        }

        return builder.build();
    }
}
