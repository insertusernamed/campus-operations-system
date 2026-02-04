package org.campusscheduler.domain.changerequest;

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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.timeslot.TimeSlot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a requested change to an existing schedule.
 */
@Entity
@Table(
        name = "schedule_change_requests",
        indexes = {
                @Index(name = "idx_change_request_status", columnList = "status"),
                @Index(name = "idx_change_request_instructor", columnList = "requested_by_instructor_id"),
                @Index(name = "idx_change_request_semester", columnList = "original_semester"),
                @Index(name = "idx_change_request_schedule", columnList = "schedule_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A requested change to an existing schedule")
public class ScheduleChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Schedule is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @NotNull(message = "Requested by instructor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_instructor_id", nullable = false)
    private Instructor requestedByInstructor;

    @NotNull(message = "Requested by role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestRole requestedByRole;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestStatus status;

    @NotNull(message = "Reason category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeRequestReason reasonCategory;

    @Size(max = 500, message = "Reason details must not exceed 500 characters")
    @Column(length = 500)
    private String reasonDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_room_id")
    private Room proposedRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_time_slot_id")
    private TimeSlot proposedTimeSlot;

    @NotNull(message = "Original room ID is required")
    @Column(name = "original_room_id", nullable = false)
    private Long originalRoomId;

    @NotNull(message = "Original time slot ID is required")
    @Column(name = "original_time_slot_id", nullable = false)
    private Long originalTimeSlotId;

    @NotNull(message = "Original semester is required")
    @Size(max = 50, message = "Semester must not exceed 50 characters")
    @Column(name = "original_semester", nullable = false, length = 50)
    private String originalSemester;

    @Size(max = 500, message = "Decision note must not exceed 500 characters")
    @Column(length = 500)
    private String decisionNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime appliedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleChangeRequest that = (ScheduleChangeRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
