package org.campusscheduler.solver;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * Unit tests for schedule constraints using Timefold ConstraintVerifier.
 */
class ScheduleConstraintProviderTest {

    private ConstraintVerifier<ScheduleConstraintProvider, ScheduleSolution> constraintVerifier;

    private Room room1;
    private Room room2;
    private TimeSlot slot1;
    private TimeSlot slot2;
    private Instructor instructor1;
    private Instructor instructor2;
    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        constraintVerifier = ConstraintVerifier.build(
                new ScheduleConstraintProvider(),
                ScheduleSolution.class,
                ScheduleAssignment.class);

        // Create test data
        room1 = Room.builder().id(1L).roomNumber("101").capacity(50).type(Room.RoomType.CLASSROOM).build();
        room2 = Room.builder().id(2L).roomNumber("201").capacity(100).type(Room.RoomType.LECTURE_HALL).build();

        slot1 = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0)).label("Mon 9-10").build();
        slot2 = TimeSlot.builder().id(2L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0)).label("Mon 10-11").build();

        instructor1 = Instructor.builder().id(1L).firstName("John").lastName("Doe").email("john@test.edu")
                .department("CS").build();
        instructor2 = Instructor.builder().id(2L).firstName("Jane").lastName("Smith").email("jane@test.edu")
                .department("Math").build();

        course1 = Course.builder().id(1L).code("CS101").name("Intro CS").enrollmentCapacity(30).instructor(instructor1)
                .department("Computer Science").build();
        course2 = Course.builder().id(2L).code("CS201").name("Data Structures").enrollmentCapacity(35)
                .instructor(instructor1).department("Computer Science").build();
    }

    @Nested
    @DisplayName("Room Conflict Constraint")
    class RoomConflict {

        @Test
        @DisplayName("should penalize two courses in same room at same time")
        void shouldPenalizeSameRoomSameTime() {
            ScheduleAssignment a1 = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();
            ScheduleAssignment a2 = ScheduleAssignment.builder()
                    .id(2L).course(course2).room(room1).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomConflict)
                    .given(a1, a2)
                    .penalizesBy(1);
        }

        @Test
        @DisplayName("should not penalize different rooms")
        void shouldNotPenalizeDifferentRooms() {
            ScheduleAssignment a1 = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();
            ScheduleAssignment a2 = ScheduleAssignment.builder()
                    .id(2L).course(course2).room(room2).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomConflict)
                    .given(a1, a2)
                    .penalizesBy(0);
        }

        @Test
        @DisplayName("should not penalize different time slots")
        void shouldNotPenalizeDifferentTimeSlots() {
            ScheduleAssignment a1 = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();
            ScheduleAssignment a2 = ScheduleAssignment.builder()
                    .id(2L).course(course2).room(room1).timeSlot(slot2).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomConflict)
                    .given(a1, a2)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Room Capacity Constraint")
    class RoomCapacity {

        @Test
        @DisplayName("should penalize when enrollment exceeds capacity")
        void shouldPenalizeOverCapacity() {
            Course largeCourse = Course.builder().id(3L).code("CS301").name("Large").enrollmentCapacity(70)
                    .instructor(instructor1).build();
            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(largeCourse).room(room1).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomCapacity)
                    .given(assignment)
                    .penalizesBy(20); // 70 - 50 = 20
        }

        @Test
        @DisplayName("should not penalize when room fits course")
        void shouldNotPenalizeWhenFits() {
            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomCapacity)
                    .given(assignment)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Room Availability Constraint")
    class RoomAvailability {

        @Test
        @DisplayName("should penalize assignment using unavailable room")
        void shouldPenalizeUnavailableRoom() {
            Room unavailableRoom = Room.builder()
                    .id(9L)
                    .roomNumber("MAINT-1")
                    .capacity(50)
                    .type(Room.RoomType.CLASSROOM)
                    .availabilityStatus(Room.AvailabilityStatus.MAINTENANCE)
                    .build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(unavailableRoom).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomAvailability)
                    .given(assignment)
                    .penalizesBy(1);
        }

        @Test
        @DisplayName("should not penalize assignment using available room")
        void shouldNotPenalizeAvailableRoom() {
            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomAvailability)
                    .given(assignment)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Instructor Conflict Constraint")
    class InstructorConflict {

        @Test
        @DisplayName("should penalize same instructor teaching two courses at same time")
        void shouldPenalizeSameInstructorSameTime() {
            ScheduleAssignment a1 = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();
            ScheduleAssignment a2 = ScheduleAssignment.builder()
                    .id(2L).course(course2).room(room2).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::instructorConflict)
                    .given(a1, a2)
                    .penalizesBy(1);
        }

        @Test
        @DisplayName("should not penalize different instructors")
        void shouldNotPenalizeDifferentInstructors() {
            Course course3 = Course.builder().id(3L).code("MATH101").name("Calculus").enrollmentCapacity(40)
                    .instructor(instructor2).build();

            ScheduleAssignment a1 = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(slot1).semester("Fall 2026").build();
            ScheduleAssignment a2 = ScheduleAssignment.builder()
                    .id(2L).course(course3).room(room2).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::instructorConflict)
                    .given(a1, a2)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Room Type Mismatch Constraint")
    class RoomTypeMismatch {

        @Test
        @DisplayName("should penalize science course not in lab")
        void shouldPenalizeScienceCourseNotInLab() {
            Course chemistryCourse = Course.builder().id(3L).code("CHEM101").name("Intro Chemistry")
                    .enrollmentCapacity(25).instructor(instructor1).department("Chemistry").build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(chemistryCourse).room(room1).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomTypeMismatch)
                    .given(assignment)
                    .penalizesBy(1); // Chemistry in CLASSROOM = mismatch
        }

        @Test
        @DisplayName("should not penalize science course in lab")
        void shouldNotPenalizeScienceCourseInLab() {
            Room labRoom = Room.builder().id(3L).roomNumber("LAB101").capacity(30).type(Room.RoomType.LAB).build();
            Course physicsCourse = Course.builder().id(3L).code("PHYS101").name("Physics Lab")
                    .enrollmentCapacity(25).instructor(instructor1).department("Physics").build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(physicsCourse).room(labRoom).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomTypeMismatch)
                    .given(assignment)
                    .penalizesBy(0);
        }

        @Test
        @DisplayName("should penalize large course not in lecture hall")
        void shouldPenalizeLargeCourseNotInLectureHall() {
            Course largeCourse = Course.builder().id(3L).code("CS101").name("Intro CS Large")
                    .enrollmentCapacity(100).instructor(instructor1).department("Computer Science").build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(largeCourse).room(room1).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomTypeMismatch)
                    .given(assignment)
                    .penalizesBy(1); // 100 students in CLASSROOM = mismatch
        }

        @Test
        @DisplayName("should not penalize large course in lecture hall")
        void shouldNotPenalizeLargeCourseInLectureHall() {
            Course largeCourse = Course.builder().id(3L).code("CS101").name("Intro CS Large")
                    .enrollmentCapacity(100).instructor(instructor1).department("Computer Science").build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(largeCourse).room(room2).timeSlot(slot1).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomTypeMismatch)
                    .given(assignment)
                    .penalizesBy(0); // 100 students in LECTURE_HALL = match
        }
    }

    @Nested
    @DisplayName("Department Building Affinity Constraint")
    class DepartmentBuildingAffinity {

        @Test
        @DisplayName("should penalize course scheduled outside preferred building codes")
        void shouldPenalizeOutsidePreferredBuilding() {
            Room artRoom = Room.builder().id(4L).roomNumber("301").capacity(45).type(Room.RoomType.CLASSROOM).build();
            artRoom.setBuilding(org.campusscheduler.domain.building.Building.builder()
                    .id(4L)
                    .code("ART")
                    .name("Arts Center")
                    .build());

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L)
                    .course(course1)
                    .room(artRoom)
                    .timeSlot(slot1)
                    .semester("Fall 2026")
                    .preferredBuildingCodes(Set.of("CSC", "ENG"))
                    .build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::departmentBuildingAffinity)
                    .given(assignment)
                    .penalizesBy(1);
        }

        @Test
        @DisplayName("should not penalize course in preferred building")
        void shouldNotPenalizeInsidePreferredBuilding() {
            room1.setBuilding(org.campusscheduler.domain.building.Building.builder()
                    .id(1L)
                    .code("ENG")
                    .name("Engineering Hall")
                    .build());

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L)
                    .course(course1)
                    .room(room1)
                    .timeSlot(slot1)
                    .semester("Fall 2026")
                    .preferredBuildingCodes(Set.of("CSC", "ENG"))
                    .build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::departmentBuildingAffinity)
                    .given(assignment)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Room Overutilization Constraint")
    class RoomOverutilization {

        @Test
        @DisplayName("should penalize usage beyond soft cap for classroom")
        void shouldPenalizeOverusedClassroom() {
            ScheduleAssignment[] assignments = new ScheduleAssignment[22];
            for (int i = 0; i < assignments.length; i++) {
                assignments[i] = ScheduleAssignment.builder()
                        .id((long) (i + 1))
                        .course(course1)
                        .room(room1)
                        .timeSlot(slot1)
                        .semester("Fall 2026")
                        .build();
            }

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomOverutilization)
                    .given(assignments)
                    .penalizesBy(1);
        }

        @Test
        @DisplayName("should not penalize usage at soft cap")
        void shouldNotPenalizeAtCap() {
            ScheduleAssignment[] assignments = new ScheduleAssignment[21];
            for (int i = 0; i < assignments.length; i++) {
                assignments[i] = ScheduleAssignment.builder()
                        .id((long) (i + 1))
                        .course(course1)
                        .room(room1)
                        .timeSlot(slot1)
                        .semester("Fall 2026")
                        .build();
            }

            constraintVerifier.verifyThat(ScheduleConstraintProvider::roomOverutilization)
                    .given(assignments)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Time Slot Preference Constraint")
    class TimeSlotPreference {

        @Test
        @DisplayName("should penalize early monday slot")
        void shouldPenalizeEarlyMonday() {
            TimeSlot earlyMonday = TimeSlot.builder()
                    .id(10L)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(9, 0))
                    .label("Mon 8-9")
                    .build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(earlyMonday).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::timeSlotPreference)
                    .given(assignment)
                    .penalizesBy(2); // 2 (early slot) + 0 (Monday)
        }

        @Test
        @DisplayName("should not penalize preferred midday tuesday slot")
        void shouldNotPenalizePreferredMidday() {
            TimeSlot preferred = TimeSlot.builder()
                    .id(11L)
                    .dayOfWeek(DayOfWeek.TUESDAY)
                    .startTime(LocalTime.of(13, 0))
                    .endTime(LocalTime.of(14, 0))
                    .label("Tue 1-2")
                    .build();

            ScheduleAssignment assignment = ScheduleAssignment.builder()
                    .id(1L).course(course1).room(room1).timeSlot(preferred).semester("Fall 2026").build();

            constraintVerifier.verifyThat(ScheduleConstraintProvider::timeSlotPreference)
                    .given(assignment)
                    .penalizesBy(0);
        }
    }

    @Nested
    @DisplayName("Time Slot Overutilization Constraint")
    class TimeSlotOverutilization {

        @Test
        @DisplayName("should penalize when a slot exceeds its soft cap")
        void shouldPenalizeOverusedSlot() {
            TimeSlot lateSlot = TimeSlot.builder()
                    .id(20L)
                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                    .startTime(LocalTime.of(16, 0))
                    .endTime(LocalTime.of(17, 15))
                    .label("Wed 4-5:15")
                    .build();

            ScheduleAssignment[] assignments = new ScheduleAssignment[47];
            for (int i = 0; i < assignments.length; i++) {
                assignments[i] = ScheduleAssignment.builder()
                        .id((long) (i + 1))
                        .course(course1)
                        .room(room1)
                        .timeSlot(lateSlot)
                        .semester("Fall 2026")
                        .build();
            }

            constraintVerifier.verifyThat(ScheduleConstraintProvider::timeSlotOverutilization)
                    .given(assignments)
                    .penalizesBy(1);
        }

        @Test
        @DisplayName("should not penalize when a slot is at soft cap")
        void shouldNotPenalizeAtSoftCap() {
            TimeSlot lateSlot = TimeSlot.builder()
                    .id(21L)
                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                    .startTime(LocalTime.of(16, 0))
                    .endTime(LocalTime.of(17, 15))
                    .label("Wed 4-5:15")
                    .build();

            ScheduleAssignment[] assignments = new ScheduleAssignment[46];
            for (int i = 0; i < assignments.length; i++) {
                assignments[i] = ScheduleAssignment.builder()
                        .id((long) (i + 1))
                        .course(course1)
                        .room(room1)
                        .timeSlot(lateSlot)
                        .semester("Fall 2026")
                        .build();
            }

            constraintVerifier.verifyThat(ScheduleConstraintProvider::timeSlotOverutilization)
                    .given(assignments)
                    .penalizesBy(0);
        }
    }
}
