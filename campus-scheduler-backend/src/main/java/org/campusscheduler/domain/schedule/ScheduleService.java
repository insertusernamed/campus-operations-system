package org.campusscheduler.domain.schedule;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Schedule business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;

    /**
     * Get all schedules.
     */
    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    /**
     * Find a schedule by ID.
     */
    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }

    /**
     * Find schedules by room ID.
     */
    public List<Schedule> findByRoomId(Long roomId) {
        return scheduleRepository.findByRoomId(roomId);
    }

    /**
     * Find schedules by course ID.
     */
    public List<Schedule> findByCourseId(Long courseId) {
        return scheduleRepository.findByCourseId(courseId);
    }

    /**
     * Find schedules by time slot ID.
     */
    public List<Schedule> findByTimeSlotId(Long timeSlotId) {
        return scheduleRepository.findByTimeSlotId(timeSlotId);
    }

    /**
     * Find schedules by semester.
     */
    public List<Schedule> findBySemester(String semester) {
        return scheduleRepository.findBySemester(semester);
    }

    /**
     * Create a new schedule.
     *
     * @param courseId   the course ID
     * @param roomId     the room ID
     * @param timeSlotId the time slot ID
     * @param semester   the semester
     * @return the created schedule, or empty if course/room/timeslot not found
     * @throws ScheduleConflictException if room is already booked or capacity is
     *                                   insufficient
     */
    @Transactional
    public Optional<Schedule> create(Long courseId, Long roomId, Long timeSlotId, String semester) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(timeSlotId);
        if (timeSlotOpt.isEmpty()) {
            return Optional.empty();
        }

        Course course = courseOpt.get();
        Room room = roomOpt.get();
        TimeSlot timeSlot = timeSlotOpt.get();

        // Check room capacity
        if (room.getCapacity() < course.getEnrollmentCapacity()) {
            throw new ScheduleConflictException(
                    "Room capacity (" + room.getCapacity() + ") is insufficient for course enrollment ("
                            + course.getEnrollmentCapacity() + ")");
        }

        // Check for room conflicts
        if (hasRoomConflict(roomId, timeSlotId)) {
            throw new ScheduleConflictException(
                    "Room " + room.getRoomNumber() + " is already booked for this time slot");
        }

        Schedule schedule = Schedule.builder()
                .course(course)
                .room(room)
                .timeSlot(timeSlot)
                .semester(semester)
                .build();

        return Optional.of(scheduleRepository.save(schedule));
    }

    /**
     * Delete a schedule by ID.
     */
    @Transactional
    public boolean delete(Long id) {
        if (scheduleRepository.existsById(id)) {
            scheduleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Check if a room has a conflict for a given time slot.
     */
    public boolean hasRoomConflict(Long roomId, Long timeSlotId) {
        List<Schedule> existing = scheduleRepository.findByRoomIdAndTimeSlotId(roomId, timeSlotId);
        return !existing.isEmpty();
    }
}
