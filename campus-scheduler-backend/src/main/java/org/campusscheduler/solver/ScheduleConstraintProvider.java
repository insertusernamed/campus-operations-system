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
 * - Room type mismatch: Penalize science courses not in labs, large courses not
 * in lecture halls
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
                roomTypeMismatch(factory),
                departmentBuildingAffinity(factory)
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
                .filter((a1, a2) -> a1.getRoom() != null && a2.getRoom() != null &&
                        a1.getTimeSlot() != null && a2.getTimeSlot() != null)
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
                Joiners.equal(assignment -> {
                    var course = assignment.getCourse();
                    return course != null && course.getInstructor() != null
                            ? course.getInstructor().getId()
                            : null;
                }),
                Joiners.equal(ScheduleAssignment::getTimeSlot))
                .filter((a1, a2) -> a1.getCourse() != null && a2.getCourse() != null &&
                        a1.getCourse().getInstructor() != null && a2.getCourse().getInstructor() != null &&
                        a1.getTimeSlot() != null && a2.getTimeSlot() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Instructor conflict");
    }

    /**
     * Penalize room type mismatches.
     * Science courses should be in labs, large courses in lecture halls.
     */
    Constraint roomTypeMismatch(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getRoom() != null &&
                        assignment.getCourse() != null &&
                        isRoomTypeMismatch(assignment))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Room type mismatch");
    }

    /**
     * Favor placing courses in department-aligned buildings when available.
     */
    Constraint departmentBuildingAffinity(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getRoom() != null &&
                        assignment.hasPreferredBuildingCodes() &&
                        !assignment.isInPreferredBuilding())
                .penalize(HardSoftScore.ofSoft(2))
                .asConstraint("Department building affinity");
    }

    /**
     * Check if the room type is a mismatch for the course.
     */
    private boolean isRoomTypeMismatch(ScheduleAssignment assignment) {
        String department = assignment.getCourse().getDepartment();
        String roomType = assignment.getRoom().getType().name();

        // Science courses should be in LAB rooms
        if (department != null &&
                (department.contains("Chemistry") || department.contains("Biology") ||
                        department.contains("Physics"))) {
            return !"LAB".equals(roomType);
        }

        // Large courses should be in lecture halls
        if (assignment.getCourse().getEnrollmentCapacity() > 80) {
            return !"LECTURE_HALL".equals(roomType);
        }

        // No mismatch for other cases
        return false;
    }
}
