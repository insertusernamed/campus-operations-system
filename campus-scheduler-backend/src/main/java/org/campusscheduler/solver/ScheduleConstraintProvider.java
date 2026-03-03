package org.campusscheduler.solver;

import java.time.DayOfWeek;
import java.time.LocalTime;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
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
 * - Room type mismatch: Penalize science courses not in labs, large courses not in lecture halls
 * - Department affinity: Prefer department-aligned buildings
 * - Room overutilization: Avoid saturating a handful of rooms
 * - Time slot preference: Favor realistic mid-day patterns over extreme early/late clustering
 */
public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                // Hard constraints
                roomConflict(factory),
                roomCapacity(factory),
                roomAvailability(factory),
                instructorConflict(factory),
                // Soft constraints
                roomTypeMismatch(factory),
                departmentBuildingAffinity(factory),
                roomOverutilization(factory),
                timeSlotOverutilization(factory),
                timeSlotPreference(factory)
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
     * Room must be operationally available for scheduling.
     */
    Constraint roomAvailability(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getRoom() != null &&
                        assignment.getRoom().getAvailabilityStatus() != null &&
                        assignment.getRoom().getAvailabilityStatus() != org.campusscheduler.domain.room.Room.AvailabilityStatus.AVAILABLE)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room availability");
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
     * Discourage saturating specific rooms beyond realistic weekly usage.
     */
    Constraint roomOverutilization(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getRoom() != null)
                .groupBy(ScheduleAssignment::getRoom, ConstraintCollectors.count())
                .filter((room, count) -> room != null && count > roomUsageSoftCap(room))
                .penalize(HardSoftScore.ONE_SOFT,
                        (room, count) -> count - roomUsageSoftCap(room))
                .asConstraint("Room overutilization");
    }

    /**
     * Apply realistic desirability by day/time instead of forcing perfect flatness.
     */
    Constraint timeSlotPreference(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getTimeSlot() != null &&
                        assignment.getTimeSlot().getStartTime() != null &&
                        assignment.getTimeSlot().getDayOfWeek() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        assignment -> timeSlotPenalty(assignment.getTimeSlot().getDayOfWeek(),
                                assignment.getTimeSlot().getStartTime()))
                .asConstraint("Time slot preference");
    }

    /**
     * Prevent oversubscribing a small subset of preferred time slots.
     */
    Constraint timeSlotOverutilization(ConstraintFactory factory) {
        return factory.forEach(ScheduleAssignment.class)
                .filter(assignment -> assignment.getTimeSlot() != null &&
                        assignment.getTimeSlot().getStartTime() != null)
                .groupBy(ScheduleAssignment::getTimeSlot, ConstraintCollectors.count())
                .filter((timeSlot, count) -> timeSlot != null && count > timeSlotSoftCap(timeSlot.getStartTime()))
                .penalize(HardSoftScore.ONE_SOFT,
                        (timeSlot, count) -> count - timeSlotSoftCap(timeSlot.getStartTime()))
                .asConstraint("Time slot overutilization");
    }

    private int roomUsageSoftCap(org.campusscheduler.domain.room.Room room) {
        if (room == null || room.getType() == null) {
            return 20;
        }
        return switch (room.getType()) {
            case LECTURE_HALL -> 24;
            case CLASSROOM, LAB -> 21;
            case SEMINAR, CONFERENCE -> 18;
        };
    }

    private int timeSlotPenalty(DayOfWeek dayOfWeek, LocalTime startTime) {
        int timePenalty;
        if (startTime.isBefore(LocalTime.of(9, 0))) {
            timePenalty = 2;
        } else if (startTime.isBefore(LocalTime.of(10, 30))) {
            timePenalty = 1;
        } else if (startTime.isBefore(LocalTime.of(12, 0))) {
            timePenalty = 0;
        } else if (startTime.isBefore(LocalTime.of(14, 30))) {
            timePenalty = 0;
        } else if (startTime.isBefore(LocalTime.of(16, 0))) {
            timePenalty = 0;
        } else {
            timePenalty = 2;
        }

        int dayPenalty = switch (dayOfWeek) {
            case FRIDAY -> 1;
            default -> 0;
        };

        return timePenalty + dayPenalty;
    }

    private int timeSlotSoftCap(LocalTime startTime) {
        if (startTime == null) {
            return 60;
        }
        if (startTime.isBefore(LocalTime.of(9, 0))) {
            return 42;
        }
        if (startTime.isBefore(LocalTime.of(10, 30))) {
            return 56;
        }
        if (startTime.isBefore(LocalTime.of(12, 0))) {
            return 64;
        }
        if (startTime.isBefore(LocalTime.of(14, 30))) {
            return 64;
        }
        if (startTime.isBefore(LocalTime.of(16, 0))) {
            return 58;
        }
        return 46;
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
