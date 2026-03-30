package org.campusscheduler.domain.roombooking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.timeslot.TimeSlot;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Student-created booking for an otherwise unused room slot.
 */
@Entity
@Table(
        name = "room_bookings",
        indexes = {
                @Index(name = "idx_room_booking_semester", columnList = "semester"),
                @Index(name = "idx_room_booking_room_slot_semester", columnList = "room_id,time_slot_id,semester"),
                @Index(name = "idx_room_booking_booked_by", columnList = "booked_by_student_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Room is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull(message = "Time slot is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @NotBlank(message = "Semester is required")
    @Size(max = 50, message = "Semester must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String semester;

    @NotNull(message = "Booking owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_student_id", nullable = false)
    private Student bookedBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_booking_participants",
            joinColumns = @JoinColumn(name = "room_booking_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Default
    private Set<Student> participants = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void applyDefaults() {
        if (participants == null) {
            participants = new LinkedHashSet<>();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoomBooking that = (RoomBooking) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
