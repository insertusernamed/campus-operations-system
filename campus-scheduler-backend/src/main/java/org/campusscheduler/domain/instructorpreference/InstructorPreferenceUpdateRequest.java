package org.campusscheduler.domain.instructorpreference;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Upsert payload for instructor preferences.
 */
@Getter
@Setter
public class InstructorPreferenceUpdateRequest {

    private LocalTime preferredStartTime;

    private LocalTime preferredEndTime;

    @NotNull(message = "Max gap minutes is required")
    @Min(value = 0, message = "Max gap minutes must be at least 0")
    @Max(value = 360, message = "Max gap minutes must not exceed 360")
    private Integer maxGapMinutes;

    @NotNull(message = "Min travel buffer minutes is required")
    @Min(value = 0, message = "Min travel buffer minutes must be at least 0")
    @Max(value = 60, message = "Min travel buffer minutes must not exceed 60")
    private Integer minTravelBufferMinutes;

    @NotNull(message = "Avoid building hops is required")
    private Boolean avoidBuildingHops;

    private List<Long> preferredBuildingIds = new ArrayList<>();

    @Size(max = 20, message = "Required room features must not exceed 20 entries")
    private List<@Size(max = 100, message = "Feature keyword must not exceed 100 characters") String> requiredRoomFeatures = new ArrayList<>();
}
