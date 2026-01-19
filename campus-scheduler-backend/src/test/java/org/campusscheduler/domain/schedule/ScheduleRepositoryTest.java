package org.campusscheduler.domain.schedule;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for Schedule entity.
 */
@DataJpaTest
class ScheduleRepositoryTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private Course course1;
    private Course course2;
    private Room room1;
    private Room room2;
    private TimeSlot mondayMorning;
    private TimeSlot mondayAfternoon;
    private Schedule schedule1;

    @BeforeEach
    void setUp() {
        // Create building
        Building building = Building.builder()
                .code("SCI")
                .name("Science Building")
                .build();
        buildingRepository.save(building);

        // Create courses
        course1 = Course.builder()
                .code("CS101")
                .name("Intro to CS")
                .credits(3)
                .enrollmentCapacity(30)
                .build();
        courseRepository.save(course1);

        course2 = Course.builder()
                .code("CS201")
                .name("Data Structures")
                .credits(3)
                .enrollmentCapacity(25)
                .build();
        courseRepository.save(course2);

        // Create rooms
        room1 = Room.builder()
                .roomNumber("101")
                .capacity(40)
                .type(Room.RoomType.CLASSROOM)
                .building(building)
                .build();
        roomRepository.save(room1);

        room2 = Room.builder()
                .roomNumber("102")
                .capacity(30)
                .type(Room.RoomType.LAB)
                .building(building)
                .build();
        roomRepository.save(room2);

        // Create time slots
        mondayMorning = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .label("Period 1")
                .build();
        timeSlotRepository.save(mondayMorning);

        mondayAfternoon = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
                .label("Period 4")
                .build();
        timeSlotRepository.save(mondayAfternoon);

        // Create a schedule
        schedule1 = Schedule.builder()
                .course(course1)
                .room(room1)
                .timeSlot(mondayMorning)
                .semester("Spring 2026")
                .build();
        scheduleRepository.save(schedule1);
    }

    @Test
    @DisplayName("should find schedules by room ID")
    void shouldFindSchedulesByRoomId() {
        List<Schedule> result = scheduleRepository.findByRoomId(room1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourse().getCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("should find schedules by course ID")
    void shouldFindSchedulesByCourseId() {
        List<Schedule> result = scheduleRepository.findByCourseId(course1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoom().getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("should find schedules by time slot ID")
    void shouldFindSchedulesByTimeSlotId() {
        List<Schedule> result = scheduleRepository.findByTimeSlotId(mondayMorning.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("should find schedules by room and time slot")
    void shouldFindSchedulesByRoomAndTimeSlot() {
        List<Schedule> result = scheduleRepository.findByRoomIdAndTimeSlotId(
                room1.getId(), mondayMorning.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("should return empty when room is free at time slot")
    void shouldReturnEmptyWhenRoomIsFreeAtTimeSlot() {
        List<Schedule> result = scheduleRepository.findByRoomIdAndTimeSlotId(
                room1.getId(), mondayAfternoon.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find schedules by semester")
    void shouldFindSchedulesBySemester() {
        List<Schedule> result = scheduleRepository.findBySemester("Spring 2026");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("should save schedule with all relationships")
    void shouldSaveScheduleWithAllRelationships() {
        Schedule newSchedule = Schedule.builder()
                .course(course2)
                .room(room2)
                .timeSlot(mondayAfternoon)
                .semester("Spring 2026")
                .build();

        Schedule saved = scheduleRepository.save(newSchedule);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCourse().getCode()).isEqualTo("CS201");
        assertThat(saved.getRoom().getRoomNumber()).isEqualTo("102");
    }

    @Test
    @DisplayName("should delete schedule")
    void shouldDeleteSchedule() {
        Long id = schedule1.getId();
        scheduleRepository.deleteById(id);

        assertThat(scheduleRepository.findById(id)).isEmpty();
    }
}
