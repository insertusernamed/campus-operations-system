package org.campusscheduler.domain.room;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.campusscheduler.domain.building.Building;

import java.util.Objects;

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

	@Size(max = 500, message = "Features must not exceed 500 characters")
	@Column(length = 500)
	private String features;

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
