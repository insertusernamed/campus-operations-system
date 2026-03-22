package org.campusscheduler.solver;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Represents a course that needs to be scheduled to a room and time slot.
 * This is the planning entity that Timefold will optimize.
 */
@PlanningEntity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleAssignment {

    @PlanningId
    private Long id;

    /**
     * The course to be scheduled (problem fact - doesn't change).
     */
    private Course course;

    /**
     * The semester for this assignment.
     */
    private String semester;

    /**
     * The room assigned to this course (planning variable - Timefold optimizes
     * this).
     */
    @Builder.Default
    @ValueRangeProvider(id = "availableRoomRange")
    private List<Room> availableRooms = Collections.emptyList();

    /**
     * Preferred building codes for this course's department.
     */
    @Builder.Default
    private Set<String> preferredBuildingCodes = Collections.emptySet();

    /**
     * The room assigned to this course (planning variable - Timefold optimizes
     * this).
     */
    @PlanningVariable(valueRangeProviderRefs = "availableRoomRange")
    private Room room;

    /**
     * The time slot assigned to this course (planning variable - Timefold optimizes
     * this).
     */
    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    /**
     * Whether this assignment is pinned (fixed) during solving.
     */
    @PlanningPin
    private boolean pinned;

    /**
     * Check if this assignment has been initialized (room and timeSlot assigned).
     */
    public boolean isInitialized() {
        return room != null && timeSlot != null;
    }

    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    public boolean overlapsWith(ScheduleAssignment other) {
        return other != null
                && timeSlot != null
                && other.getTimeSlot() != null
                && timeSlot.overlapsWith(other.getTimeSlot());
    }

    public boolean hasPreferredBuildingCodes() {
        return preferredBuildingCodes != null && !preferredBuildingCodes.isEmpty();
    }

    public boolean isInPreferredBuilding() {
        if (!hasPreferredBuildingCodes() || room == null || room.getBuildingCode() == null) {
            return false;
        }
        return preferredBuildingCodes.contains(room.getBuildingCode().trim().toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return (course != null ? course.getCode() : "?") + " -> " +
                (room != null ? room.getRoomNumber() : "?") + " @ " +
                (timeSlot != null ? timeSlot.getLabel() : "?");
    }
}
