package org.campusscheduler.solver;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Room room;

    /**
     * The time slot assigned to this course (planning variable - Timefold optimizes
     * this).
     */
    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    /**
     * Check if this assignment has been initialized (room and timeSlot assigned).
     */
    public boolean isInitialized() {
        return room != null && timeSlot != null;
    }

    @Override
    public String toString() {
        return course.getCode() + " -> " +
                (room != null ? room.getRoomNumber() : "?") + " @ " +
                (timeSlot != null ? timeSlot.getLabel() : "?");
    }
}
