package org.campusscheduler.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

/**
 * Defines the hard and soft constraints for course scheduling.
 *
 * Hard constraints (must not be violated):
 * - Room conflict: A room can only host one course at a time
 * - Room capacity: Course enrollment must not exceed room capacity
 * - Instructor conflict: An instructor can only teach one course at a time
 *
 * Soft constraints (should be optimized):
 * - Room type preference: Labs should be in lab rooms, lectures in lecture
 * halls
 */
public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                // Hard constraints
                roomConflict(factory),
                roomCapacity(factory),
                instructorConflict(factory),
                // Soft constraints
                roomTypePreference(factory)
        };
    }

    /**
     * A room can only host one course at a time.
     * Penalize each pair of assignments that share the same room and time slot.
     */
    Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(ScheduleAssignment.class,
                Joiners.equal(ScheduleAssignment::getRoom),
                Joiners.equal(ScheduleAssignment::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    /**
     * Course enrollment must not exceed room capacity.
     */
    Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getRoom() != null &&
                        assignment.getCourse() != null &&
                        assignment.getCourse().getEnrollmentCapacity() > assignment.getRoom().getCapacity())
                .penalize(HardSoftScore.ONE_HARD,
                        assignment -> assignment.getCourse().getEnrollmentCapacity() -
                                assignment.getRoom().getCapacity())
                .asConstraint("Room capacity");
    }

    /**
     * An instructor can only teach one course at a time.
     * Penalize each pair of assignments with the same instructor at the same time.
     */
    Constraint instructorConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(ScheduleAssignment.class,
                Joiners.equal(assignment -> assignment.getCourse().getInstructor()),
                Joiners.equal(ScheduleAssignment::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Instructor conflict");
    }

    /**
     * Prefer matching room types (labs in lab rooms, etc).
     * Reward when course department matches room type expectations.
     */
    Constraint roomTypePreference(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getRoom() != null &&
                        isGoodRoomMatch(assignment))
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Room type preference");
    }

    /**
     * Check if the room type is a good match for the course.
     */
    private boolean isGoodRoomMatch(ScheduleAssignment assignment) {
        String department = assignment.getCourse().getDepartment();
        String roomType = assignment.getRoom().getType().name();

        // Labs should be in LAB rooms
        if (department != null &&
                (department.contains("Chemistry") || department.contains("Biology") ||
                        department.contains("Physics"))) {
            return "LAB".equals(roomType);
        }

        // Large courses should be in lecture halls
        if (assignment.getCourse().getEnrollmentCapacity() > 80) {
            return "LECTURE_HALL".equals(roomType);
        }

        // Small courses prefer seminars or classrooms
        if (assignment.getCourse().getEnrollmentCapacity() < 25) {
            return "SEMINAR".equals(roomType) || "CLASSROOM".equals(roomType);
        }

        return true;
    }
}
