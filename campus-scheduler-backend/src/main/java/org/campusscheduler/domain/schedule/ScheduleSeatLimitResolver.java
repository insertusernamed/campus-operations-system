package org.campusscheduler.domain.schedule;

/**
 * Shared seat-limit semantics for scheduled offerings.
 */
public final class ScheduleSeatLimitResolver {

    private ScheduleSeatLimitResolver() {
    }

    public static int resolve(Schedule schedule) {
        if (schedule == null) {
            return 0;
        }

        Integer courseCapacity = schedule.getCourse() != null ? schedule.getCourse().getEnrollmentCapacity() : null;
        Integer roomCapacity = schedule.getRoom() != null ? schedule.getRoom().getCapacity() : null;

        if (courseCapacity == null && roomCapacity == null) {
            return 0;
        }
        if (courseCapacity == null) {
            return roomCapacity;
        }
        if (roomCapacity == null) {
            return courseCapacity;
        }
        return Math.min(courseCapacity, roomCapacity);
    }
}
