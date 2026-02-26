package org.campusscheduler.generator;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.changerequest.ScheduleChangeRequestRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreference;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.campusscheduler.generator.DataGeneratorService.Contact;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationConfig;
import org.campusscheduler.generator.UniversityGeneratorService.GenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UniversityGeneratorService.
 */
@ExtendWith(MockitoExtension.class)
class UniversityGeneratorServiceTest {

	@Mock
	private DataGeneratorService dataGeneratorService;

	@Mock
	private ScheduleChangeRequestRepository scheduleChangeRequestRepository;

	@Mock
	private BuildingRepository buildingRepository;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private InstructorRepository instructorRepository;

	@Mock
	private InstructorPreferenceRepository instructorPreferenceRepository;

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private TimeSlotRepository timeSlotRepository;

	@Mock
	private jakarta.persistence.EntityManager entityManager;

	@Captor
	private ArgumentCaptor<Building> buildingCaptor;

	@Captor
	private ArgumentCaptor<Room> roomCaptor;

	@Captor
	private ArgumentCaptor<Instructor> instructorCaptor;

	@Captor
	private ArgumentCaptor<Course> courseCaptor;

	@Captor
	private ArgumentCaptor<List<InstructorPreference>> preferenceListCaptor;

	private UniversityGeneratorService service;

	@BeforeEach
	void setUp() {
		service = new UniversityGeneratorService(
				dataGeneratorService,
				scheduleChangeRequestRepository,
				buildingRepository,
				roomRepository,
				instructorRepository,
				instructorPreferenceRepository,
				courseRepository,
				scheduleRepository,
				timeSlotRepository,
				entityManager);
	}

	@Nested
	@DisplayName("GenerationConfig")
	class GenerationConfigTests {

			@Test
			@DisplayName("default config should use research-based ratios for COMMUNITY 8000 students")
			void defaultConfigShouldHaveReasonableValues() {
				GenerationConfig config = GenerationConfig.defaultConfig();

			// COMMUNITY archetype: 200 students/building, so 8000/200 = 40 buildings
				assertThat(config.archetype()).isEqualTo(UniversityArchetype.COMMUNITY);
				assertThat(config.studentPopulation()).isEqualTo(8000);
				assertThat(config.buildings()).isEqualTo(40);
				assertThat(config.academicBuildings()).isEqualTo(24); // 60% of 40
				// Demand and catalog blend for COMMUNITY at 8,000 students
				assertThat(config.courses()).isEqualTo(1334);
				assertThat(config.roomsPerBuilding()).isEqualTo(3);
			}

		@Test
		@DisplayName("small config should be smaller than default")
		void smallConfigShouldBeSmallerThanDefault() {
			GenerationConfig small = GenerationConfig.small();
			GenerationConfig def = GenerationConfig.defaultConfig();

			assertThat(small.buildings()).isLessThan(def.buildings());
			assertThat(small.courses()).isLessThan(def.courses());
		}
	}

	@Nested
	@DisplayName("generateUniversity")
	class GenerateUniversity {

		@BeforeEach
		void setUpMocks() {
			// Mock building saves to return with ID
			when(buildingRepository.save(any(Building.class)))
					.thenAnswer(inv -> {
						Building b = inv.getArgument(0);
						b.setId(1L);
						return b;
					});

			// Mock room saves
			when(roomRepository.save(any(Room.class)))
					.thenAnswer(inv -> {
						Room r = inv.getArgument(0);
						r.setId(1L);
						return r;
					});

			// Mock instructor saves
			when(instructorRepository.save(any(Instructor.class)))
					.thenAnswer(inv -> {
						Instructor i = inv.getArgument(0);
						i.setId(1L);
						return i;
					});

			// Mock course saves
			when(courseRepository.save(any(Course.class)))
					.thenAnswer(inv -> {
						Course c = inv.getArgument(0);
						c.setId(1L);
						return c;
					});

			// Mock contacts
			when(dataGeneratorService.getRandomContacts(50))
					.thenReturn(List.of(
							new Contact("John", "Doe", "john@test.edu"),
							new Contact("Jane", "Smith", "jane@test.edu")));

			when(dataGeneratorService.generateRoomNumber(any(Integer.class)))
					.thenReturn("101");

			when(dataGeneratorService.generateCapacity(any(String.class)))
					.thenReturn(30);

			when(timeSlotRepository.count()).thenReturn(30L);
		}

		@Test
		@DisplayName("should generate buildings")
		void shouldGenerateBuildings() {
			// Using 4 total buildings, 4 academic buildings to ensure all are generated
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8000, 4, 4, 9, 50, 100);

			service.generateUniversity(config);

			verify(buildingRepository, times(4)).save(buildingCaptor.capture());
			List<Building> savedBuildings = buildingCaptor.getAllValues();

			assertThat(savedBuildings).hasSize(4);
			assertThat(savedBuildings.get(0).getName()).isNotBlank();
			assertThat(savedBuildings.get(0).getCode()).isNotBlank();
		}

		@Test
		@DisplayName("should generate rooms for each building")
		void shouldGenerateRoomsForEachBuilding() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8000, 2, 2, 9, 50, 100);

			service.generateUniversity(config);

			// 2 buildings * 9 rooms per building = 18 rooms
			verify(roomRepository, atLeast(6)).save(any(Room.class));
		}

		@Test
		@DisplayName("should generate instructors from contacts")
		void shouldGenerateInstructorsFromContacts() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8000, 2, 2, 9, 50, 100);

			service.generateUniversity(config);

			verify(instructorRepository, times(2)).save(instructorCaptor.capture());
			List<Instructor> savedInstructors = instructorCaptor.getAllValues();

			assertThat(savedInstructors.get(0).getFirstName()).isEqualTo("John");
			assertThat(savedInstructors.get(0).getLastName()).isEqualTo("Doe");
		}

		@Test
		@DisplayName("should return accurate generation result")
		void shouldReturnAccurateResult() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8000, 2, 2, 9, 50, 10);

			GenerationResult result = service.generateUniversity(config);

			assertThat(result.buildings()).isEqualTo(2);
			assertThat(result.instructors()).isEqualTo(2);
			assertThat(result.courses()).isEqualTo(10);
			assertThat(result.timeSlots()).isEqualTo(30);
		}

		@Test
		@DisplayName("should generate preferences for each instructor")
		void shouldGeneratePreferencesForEachInstructor() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8000, 2, 2, 9, 50, 10);

			service.generateUniversity(config);

			verify(instructorPreferenceRepository).saveAll(preferenceListCaptor.capture());
			assertThat(preferenceListCaptor.getValue()).hasSize(2);
			assertThat(preferenceListCaptor.getValue())
					.allSatisfy(preference -> {
						assertThat(preference.getInstructor()).isNotNull();
						assertThat(preference.getPreferredStartTime()).isNotNull();
						assertThat(preference.getPreferredEndTime()).isNotNull();
						assertThat(preference.getUpdatedAt()).isNotNull();
					});
		}
	}

	@Nested
	@DisplayName("clearAll")
	class ClearAll {

		@Test
		@DisplayName("should delete all entities in correct order")
		void shouldDeleteAllEntitiesInCorrectOrder() {
			service.clearAll();

			var inOrder = inOrder(
					scheduleChangeRequestRepository,
					scheduleRepository,
					courseRepository,
					instructorPreferenceRepository,
					instructorRepository,
					roomRepository,
					buildingRepository);

			inOrder.verify(scheduleChangeRequestRepository).deleteAll();
			inOrder.verify(scheduleRepository).deleteAll();
			inOrder.verify(courseRepository).deleteAll();
			inOrder.verify(instructorPreferenceRepository).deleteAll();
			inOrder.verify(instructorRepository).deleteAll();
			inOrder.verify(roomRepository).deleteAll();
			inOrder.verify(buildingRepository).deleteAll();
		}
	}
}
