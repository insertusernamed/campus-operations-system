package org.campusscheduler.domain.schedule;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * API response model for schedule payloads. Mirrors the existing schedule
 * object shape while allowing computed seat metrics to be added without
 * changing core entity classes.
 */
public record ScheduleResponse(
        Long id,
        CourseSummary course,
        RoomSummary room,
        TimeSlotSummary timeSlot,
        String semester,
        Integer filledSeats,
        Integer seatLimit,
        Integer remainingSeats,
        Integer waitlistCount) {

    public static ScheduleResponse from(Schedule schedule) {
        return from(schedule, null);
    }

    public static ScheduleResponse from(Schedule schedule, ScheduleSeatSummary seatSummary) {
        if (schedule == null) {
            return null;
        }
        return new ScheduleResponse(
                schedule.getId(),
                CourseSummary.from(schedule.getCourse()),
                RoomSummary.from(schedule.getRoom()),
                TimeSlotSummary.from(schedule.getTimeSlot()),
                schedule.getSemester(),
                seatSummary != null ? seatSummary.filledSeats() : null,
                seatSummary != null ? seatSummary.seatLimit() : null,
                seatSummary != null ? seatSummary.remainingSeats() : null,
                seatSummary != null ? seatSummary.waitlistCount() : null);
    }

    public record ScheduleSeatSummary(
            int filledSeats,
            int seatLimit,
            int remainingSeats,
            int waitlistCount) {
    }

    public record CourseSummary(
            Long id,
            String code,
            String name,
            String description,
            Integer credits,
            Integer enrollmentCapacity,
            String department,
            InstructorSummary instructor) {

        public static CourseSummary from(Course course) {
            if (course == null) {
                return null;
            }
            return new CourseSummary(
                    course.getId(),
                    course.getCode(),
                    course.getName(),
                    course.getDescription(),
                    course.getCredits(),
                    course.getEnrollmentCapacity(),
                    course.getDepartment(),
                    InstructorSummary.from(course.getInstructor()));
        }
    }

    public record InstructorSummary(
            Long id,
            String firstName,
            String lastName,
            String email,
            String department) {

        public static InstructorSummary from(Instructor instructor) {
            if (instructor == null) {
                return null;
            }
            return new InstructorSummary(
                    instructor.getId(),
                    instructor.getFirstName(),
                    instructor.getLastName(),
                    instructor.getEmail(),
                    instructor.getDepartment());
        }
    }

    public record RoomSummary(
            Long id,
            String roomNumber,
            Integer capacity,
            Room.RoomType type,
            Room.AvailabilityStatus availabilityStatus,
            String features,
            Set<String> featureSet,
            Set<String> accessibilityFlags,
            String operationalNotes,
            LocalDate lastInspectionDate,
            Long buildingId,
            String buildingCode,
            String buildingName) {

        public static RoomSummary from(Room room) {
            if (room == null) {
                return null;
            }
            return new RoomSummary(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getCapacity(),
                    room.getType(),
                    room.getAvailabilityStatus(),
                    room.getFeatures(),
                    room.getFeatureSet() == null ? Set.of() : new LinkedHashSet<>(room.getFeatureSet()),
                    room.getAccessibilityFlags() == null ? Set.of() : new LinkedHashSet<>(room.getAccessibilityFlags()),
                    room.getOperationalNotes(),
                    room.getLastInspectionDate(),
                    room.getBuildingId(),
                    room.getBuildingCode(),
                    room.getBuildingName());
        }
    }

    public record TimeSlotSummary(
            Long id,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String label) {

        public static TimeSlotSummary from(TimeSlot timeSlot) {
            if (timeSlot == null) {
                return null;
            }
            return new TimeSlotSummary(
                    timeSlot.getId(),
                    timeSlot.getDayOfWeek(),
                    timeSlot.getStartTime(),
                    timeSlot.getEndTime(),
                    timeSlot.getLabel());
        }
    }
}
