package org.campusscheduler.domain.instructorpreference;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.campusscheduler.domain.instructor.Instructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Preference profile used to personalize instructor scheduling ergonomics.
 */
@Entity
@Table(
        name = "instructor_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_instructor_preference_instructor", columnNames = "instructor_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false, unique = true)
    private Instructor instructor;

    @Column(name = "preferred_start_time")
    private LocalTime preferredStartTime;

    @Column(name = "preferred_end_time")
    private LocalTime preferredEndTime;

    @Column(name = "max_gap_minutes", nullable = false)
    private Integer maxGapMinutes;

    @Column(name = "min_travel_buffer_minutes", nullable = false)
    private Integer minTravelBufferMinutes;

    @Column(name = "avoid_building_hops", nullable = false)
    private boolean avoidBuildingHops;

    @ElementCollection
    @CollectionTable(name = "instructor_preference_buildings", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "building_id", nullable = false)
    @Builder.Default
    private Set<Long> preferredBuildingIds = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "instructor_preference_room_features", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "feature_keyword", nullable = false, length = 100)
    @Builder.Default
    private Set<String> requiredRoomFeatures = new LinkedHashSet<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InstructorPreference that = (InstructorPreference) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
