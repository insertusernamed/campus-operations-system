package org.campusscheduler.solver;

import java.util.List;

import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The planning solution for course scheduling.
 * Contains all problem facts (rooms, time slots) and planning entities
 * (assignments).
 */
@PlanningSolution
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSolution {

    /**
     * Available rooms for scheduling.
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<Room> rooms;

    /**
     * Available time slots for scheduling.
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeSlotRange")
    private List<TimeSlot> timeSlots;

    /**
     * The course assignments to be optimized.
     */
    @PlanningEntityCollectionProperty
    private List<ScheduleAssignment> assignments;

    /**
     * The optimization score.
     * Hard score = constraint violations (must be 0 for valid solution)
     * Soft score = optimization goals (higher is better)
     */
    @PlanningScore
    private HardSoftScore score;

    /**
     * Semester for this solution.
     */
    private String semester;

    @Override
    public String toString() {
        return "ScheduleSolution{" +
                "assignments=" + (assignments != null ? assignments.size() : 0) +
                ", score=" + score +
                '}';
    }
}
