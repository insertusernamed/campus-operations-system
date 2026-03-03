package org.campusscheduler.domain.room;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.campusscheduler.domain.building.Building;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a room within a building that can be scheduled.
 */
@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Room number is required")
	@Size(max = 20, message = "Room number must not exceed 20 characters")
	@Column(nullable = false, length = 20)
	private String roomNumber;

	@NotNull(message = "Capacity is required")
	@Min(value = 1, message = "Capacity must be at least 1")
	@Column(nullable = false)
	private Integer capacity;

	@NotNull(message = "Room type is required")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomType type;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	@Default
	private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

	@Size(max = 500, message = "Features must not exceed 500 characters")
	@Column(length = 500)
	private String features;

	@Size(max = 25, message = "Feature set must not exceed 25 entries")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "room_feature_set", joinColumns = @JoinColumn(name = "room_id"))
	@Column(name = "feature_tag", length = 80)
	@Default
	private Set<@Size(max = 80, message = "Feature tag must not exceed 80 characters") String> featureSet = new LinkedHashSet<>();

	@Size(max = 25, message = "Accessibility flags must not exceed 25 entries")
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "room_accessibility_flags", joinColumns = @JoinColumn(name = "room_id"))
	@Column(name = "accessibility_flag", length = 80)
	@Default
	private Set<@Size(max = 80, message = "Accessibility flag must not exceed 80 characters") String> accessibilityFlags = new LinkedHashSet<>();

	@Size(max = 1000, message = "Operational notes must not exceed 1000 characters")
	@Column(length = 1000)
	private String operationalNotes;

	@PastOrPresent(message = "Last inspection date cannot be in the future")
	@Column(name = "last_inspection_date")
	private LocalDate lastInspectionDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "building_id", nullable = false)
	@JsonIgnore
	private Building building;

	/**
	 * Types of rooms available for scheduling.
	 */
	public enum RoomType {
		CLASSROOM,
		LECTURE_HALL,
		LAB,
		SEMINAR,
		CONFERENCE
	}

	public enum AvailabilityStatus {
		AVAILABLE,
		MAINTENANCE,
		OUT_OF_SERVICE
	}

	@PrePersist
	void applyDefaults() {
		if (availabilityStatus == null) {
			availabilityStatus = AvailabilityStatus.AVAILABLE;
		}
		if (featureSet == null) {
			featureSet = new LinkedHashSet<>();
		}
		if (accessibilityFlags == null) {
			accessibilityFlags = new LinkedHashSet<>();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Room room = (Room) o;
		return Objects.equals(roomNumber, room.roomNumber) &&
				Objects.equals(building != null ? building.getId() : null,
						room.building != null ? room.building.getId() : null);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roomNumber, building != null ? building.getId() : null);
	}

	// Expose building details for JSON serialization since the relationship is
	// ignored
	public Long getBuildingId() {
		return building != null ? building.getId() : null;
	}

	public String getBuildingCode() {
		return building != null ? building.getCode() : null;
	}

	public String getBuildingName() {
		return building != null ? building.getName() : null;
	}
}
