package org.campusscheduler.domain.instructorinsight;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.changerequest.ScheduleChangeRequestRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceSettings;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceService;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorInsightsServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private InstructorPreferenceService preferenceService;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorPreferenceRepository instructorPreferenceRepository;

    @Mock
    private ScheduleChangeRequestRepository changeRequestRepository;

    @InjectMocks
    private InstructorInsightsService insightsService;

    @Test
    void detectsLargeGapIssue() {
        InstructorPreferenceSettings settings = new InstructorPreferenceSettings(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                60,
                15,
                true,
                List.of(),
                List.of());

        Schedule first = schedule(1L, DayOfWeek.MONDAY, "09:00", "10:00", 1L, "SCI", "Projector");
        Schedule second = schedule(2L, DayOfWeek.MONDAY, "14:00", "15:00", 1L, "SCI", "Projector");

        when(preferenceService.getEffectiveSettings(10L)).thenReturn(Optional.of(settings));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026"))
                .thenReturn(List.of(first, second));

        Optional<List<InstructorFrictionIssueResponse>> response = insightsService.findFrictions(10L, "Fall 2026");

        assertThat(response).isPresent();
        assertThat(response.get())
                .anyMatch(item -> item.type() == InstructorFrictionType.LARGE_GAP
                        && item.scheduleId().equals(2L)
                        && item.recommendedIssue() == RecommendedIssue.GAP_TOO_LARGE_BEFORE);
    }

    @Test
    void detectsTightBuildingHopIssue() {
        InstructorPreferenceSettings settings = new InstructorPreferenceSettings(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                180,
                20,
                true,
                List.of(),
                List.of());

        Schedule first = schedule(1L, DayOfWeek.TUESDAY, "10:00", "11:00", 1L, "SCI", "Projector");
        Schedule second = schedule(2L, DayOfWeek.TUESDAY, "11:10", "12:00", 2L, "ENG", "Projector");

        when(preferenceService.getEffectiveSettings(10L)).thenReturn(Optional.of(settings));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026"))
                .thenReturn(List.of(first, second));

        Optional<List<InstructorFrictionIssueResponse>> response = insightsService.findFrictions(10L, "Fall 2026");

        assertThat(response).isPresent();
        assertThat(response.get())
                .anyMatch(item -> item.type() == InstructorFrictionType.TIGHT_BUILDING_HOP
                        && item.recommendedIssue() == RecommendedIssue.BACK_TO_BACK_TRAVEL);
    }

    @Test
    void detectsOutsidePreferredWindowIssue() {
        InstructorPreferenceSettings settings = new InstructorPreferenceSettings(
                LocalTime.of(10, 0),
                LocalTime.of(16, 0),
                180,
                15,
                true,
                List.of(),
                List.of());

        Schedule early = schedule(3L, DayOfWeek.WEDNESDAY, "08:00", "09:15", 1L, "SCI", "Projector");

        when(preferenceService.getEffectiveSettings(10L)).thenReturn(Optional.of(settings));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026"))
                .thenReturn(List.of(early));

        Optional<List<InstructorFrictionIssueResponse>> response = insightsService.findFrictions(10L, "Fall 2026");

        assertThat(response).isPresent();
        assertThat(response.get())
                .anyMatch(item -> item.type() == InstructorFrictionType.OUTSIDE_PREFERRED_WINDOW
                        && item.recommendedIssue() == RecommendedIssue.TIME_OF_DAY_PREFERENCE);
    }

    @Test
    void detectsRoomFeatureMismatchIssue() {
        InstructorPreferenceSettings settings = new InstructorPreferenceSettings(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                180,
                15,
                true,
                List.of(),
                List.of("projector", "microphone"));

        Schedule schedule = schedule(4L, DayOfWeek.THURSDAY, "13:00", "14:00", 1L, "SCI", "Projector");

        when(preferenceService.getEffectiveSettings(10L)).thenReturn(Optional.of(settings));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026"))
                .thenReturn(List.of(schedule));

        Optional<List<InstructorFrictionIssueResponse>> response = insightsService.findFrictions(10L, "Fall 2026");

        assertThat(response).isPresent();
        assertThat(response.get())
                .anyMatch(item -> item.type() == InstructorFrictionType.ROOM_FEATURE_MISMATCH
                        && item.recommendedIssue() == RecommendedIssue.ROOM_EQUIPMENT_MISMATCH);
    }

    @Test
    void returnsNoFalsePositivesWhenScheduleComplies() {
        InstructorPreferenceSettings settings = new InstructorPreferenceSettings(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                180,
                10,
                true,
                List.of(1L),
                List.of("projector"));

        Schedule first = schedule(5L, DayOfWeek.FRIDAY, "09:00", "10:00", 1L, "SCI", "Projector, Microphone");
        Schedule second = schedule(6L, DayOfWeek.FRIDAY, "11:00", "12:00", 1L, "SCI", "Projector, Microphone");

        when(preferenceService.getEffectiveSettings(10L)).thenReturn(Optional.of(settings));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026"))
                .thenReturn(List.of(first, second));

        Optional<List<InstructorFrictionIssueResponse>> response = insightsService.findFrictions(10L, "Fall 2026");

        assertThat(response).isPresent();
        assertThat(response.get()).isEmpty();
    }

    @Test
    void supportsAliasMatchingForRoomFeatures() {
        InstructorPreferenceSettings settings = new InstructorPreferenceSettings(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                180,
                10,
                true,
                List.of(1L),
                List.of("lecture capture"));

        Schedule schedule = schedule(7L, DayOfWeek.MONDAY, "09:00", "10:00", 1L, "SCI", "Projector, Recording Equipment");

        when(preferenceService.getEffectiveSettings(10L)).thenReturn(Optional.of(settings));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026"))
                .thenReturn(List.of(schedule));

        Optional<List<InstructorFrictionIssueResponse>> response = insightsService.findFrictions(10L, "Fall 2026");

        assertThat(response).isPresent();
        assertThat(response.get()).isEmpty();
    }

    @Test
    void buildsSummaryAndQueueFromOperationalMetrics() {
        Instructor instructor = Instructor.builder()
                .id(10L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@campus.edu")
                .department("Computer Science")
                .build();

        Course assigned = Course.builder()
                .id(101L)
                .code("CS101")
                .name("Intro")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .instructor(instructor)
                .build();

        Course unassigned = Course.builder()
                .id(102L)
                .code("CS201")
                .name("Data Structures")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .instructor(null)
                .build();

        when(instructorRepository.findAll()).thenReturn(List.of(instructor));
        when(courseRepository.findAll()).thenReturn(List.of(assigned, unassigned));
        when(scheduleRepository.findBySemester("Fall 2026")).thenReturn(List.of());
        when(instructorPreferenceRepository.findAll()).thenReturn(List.of());

        InstructorInsightsSummaryResponse summary = insightsService.getSummary("Fall 2026");
        List<InstructorQueueRowResponse> queue = insightsService.getQueue("Fall 2026", "coverage-risk", "Computer Science");

        assertThat(summary.totalInstructors()).isEqualTo(1);
        assertThat(summary.noCurrentAssignment()).isEqualTo(0);
        assertThat(summary.preferenceSetupIncomplete()).isEqualTo(1);
        assertThat(summary.departmentsWithCoverageRisk()).isEqualTo(1);

        assertThat(queue).hasSize(1);
        assertThat(queue.getFirst().status()).isEqualTo(InstructorOperationalStatus.COVERAGE_RISK);
        assertThat(queue.getFirst().loadStatus()).isEqualTo(InstructorLoadStatus.UNDER);
    }

    @Test
    void returnsInstructorWorkbench() {
        Instructor instructor = Instructor.builder()
                .id(10L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@campus.edu")
                .department("Computer Science")
                .officeNumber("SCI-201")
                .build();

        Course assigned = Course.builder()
                .id(101L)
                .code("CS101")
                .name("Intro")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .instructor(instructor)
                .build();

        Schedule schedule = schedule(1L, DayOfWeek.MONDAY, "09:00", "10:00", 1L, "SCI", "Projector");

        when(instructorRepository.findById(10L)).thenReturn(Optional.of(instructor));
        when(instructorRepository.findAll()).thenReturn(List.of(instructor));
        when(courseRepository.findAll()).thenReturn(List.of(assigned));
        when(courseRepository.findByInstructorId(10L)).thenReturn(List.of(assigned));
        when(scheduleRepository.findBySemester("Fall 2026")).thenReturn(List.of(schedule));
        when(scheduleRepository.findByCourseInstructorIdAndSemester(10L, "Fall 2026")).thenReturn(List.of(schedule));
        when(scheduleRepository.findByCourseInstructorId(10L)).thenReturn(List.of(schedule));
        when(instructorPreferenceRepository.findAll()).thenReturn(List.of());
        when(changeRequestRepository.findByFilters(null, 10L, "Fall 2026", null)).thenReturn(List.of());

        Optional<InstructorWorkbenchResponse> workbench = insightsService.getWorkbench(10L, "Fall 2026");

        assertThat(workbench).isPresent();
        assertThat(workbench.get().instructorId()).isEqualTo(10L);
        assertThat(workbench.get().assignedCourses()).hasSize(1);
        assertThat(workbench.get().weeklyDensity()).isNotEmpty();
    }

    private static Schedule schedule(
            Long scheduleId,
            DayOfWeek day,
            String start,
            String end,
            Long buildingId,
            String buildingCode,
            String features) {
        Instructor instructor = Instructor.builder().id(10L).build();
        Course course = Course.builder()
                .id(100L + scheduleId)
                .code("CS" + scheduleId)
                .name("Course " + scheduleId)
                .credits(3)
                .instructor(instructor)
                .enrollmentCapacity(30)
                .build();

        Building building = Building.builder()
                .id(buildingId)
                .code(buildingCode)
                .name(buildingCode)
                .build();

        Room room = Room.builder()
                .id(200L + scheduleId)
                .building(building)
                .roomNumber("R" + scheduleId)
                .capacity(40)
                .type(Room.RoomType.CLASSROOM)
                .features(features)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(300L + scheduleId)
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .label("Slot " + scheduleId)
                .build();

        return Schedule.builder()
                .id(scheduleId)
                .course(course)
                .room(room)
                .timeSlot(timeSlot)
                .semester("Fall 2026")
                .build();
    }
}
