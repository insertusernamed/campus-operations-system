package org.campusscheduler.generator;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.changerequest.ScheduleChangeRequestRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreference;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.student.StudentRepository;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
	private StudentRepository studentRepository;

	@Mock
	private EnrollmentRepository enrollmentRepository;

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
	private ArgumentCaptor<List<Student>> studentListCaptor;

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
				studentRepository,
				enrollmentRepository,
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
			AtomicLong courseIdSequence = new AtomicLong(1);

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
						c.setId(courseIdSequence.getAndIncrement());
						return c;
					});

			when(studentRepository.saveAll(anyCollection()))
					.thenAnswer(inv -> inv.getArgument(0));

			// Mock contacts
			when(dataGeneratorService.getRandomContacts(50))
					.thenReturn(List.of(
							new Contact("John", "Doe", "john@test.edu"),
							new Contact("Jane", "Smith", "jane@test.edu")));
			when(dataGeneratorService.getRandomContactsExcluding(any(Integer.class), anyCollection()))
					.thenReturn(List.of(
							new Contact("Alex", "Rivera", "alex@test.edu"),
							new Contact("Sam", "Chen", "sam@test.edu")));
			when(dataGeneratorService.generateStudentNumber(any(Integer.class)))
					.thenAnswer(inv -> "S%08d".formatted(inv.<Integer>getArgument(0) + 1));
			when(dataGeneratorService.generateStudentEmail(any(Contact.class), any(Integer.class)))
					.thenAnswer(inv -> {
						Contact contact = inv.getArgument(0);
						int sequence = inv.getArgument(1);
						return "%s.%s.s%05d@students.campusscheduler.edu"
								.formatted(
										contact.firstName().toLowerCase(),
										contact.lastName().toLowerCase(),
										sequence + 1);
					});

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
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8, 2, 2, 9, 50, 10);

			GenerationResult result = service.generateUniversity(config);
			verify(studentRepository).saveAll(studentListCaptor.capture());
			long expectedDemandCount = studentListCaptor.getValue().stream()
					.map(Student::getPreferredCourseIds)
					.mapToLong(List::size)
					.sum();

			assertThat(result.buildings()).isEqualTo(2);
			assertThat(result.instructors()).isEqualTo(2);
			assertThat(result.courses()).isEqualTo(10);
			assertThat(result.students()).isEqualTo(8);
			assertThat(result.generatedDemandCount()).isEqualTo(expectedDemandCount);
			assertThat(result.timeSlots()).isEqualTo(30);
		}

		@Test
		@DisplayName("should generate students from contacts not used by instructors")
		void shouldGenerateStudentsFromUnusedContacts() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 4, 2, 2, 9, 50, 10);

			service.generateUniversity(config);

			verify(studentRepository).saveAll(studentListCaptor.capture());
			assertThat(studentListCaptor.getValue()).hasSize(4);
			assertThat(studentListCaptor.getValue())
					.extracting(Student::getEmail)
					.allMatch(email -> email.endsWith("@students.campusscheduler.edu"));
			assertThat(studentListCaptor.getValue())
					.extracting(Student::getStudentNumber)
					.containsExactly("S00000001", "S00000002", "S00000003", "S00000004");
			assertThat(studentListCaptor.getValue())
					.allSatisfy(student -> {
						assertThat(student.getTargetCourseLoad()).isBetween(3, 5);
						assertThat(student.getPreferredCourseIds()).hasSizeGreaterThanOrEqualTo(student.getTargetCourseLoad());
						assertThat(student.getPreferredCourseIds()).doesNotHaveDuplicates();
					});
		}

		@Test
		@DisplayName("should bias course preferences toward student department")
		void shouldBiasCoursePreferencesTowardStudentDepartment() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 8, 2, 2, 9, 50, 48);

			service.generateUniversity(config);

			verify(courseRepository, times(48)).save(courseCaptor.capture());
			verify(studentRepository).saveAll(studentListCaptor.capture());
			Map<Long, String> courseDepartments = courseCaptor.getAllValues().stream()
					.collect(Collectors.toMap(Course::getId, Course::getDepartment, (left, right) -> left));
			Map<String, Long> coursesPerDepartment = courseCaptor.getAllValues().stream()
					.collect(Collectors.groupingBy(Course::getDepartment, Collectors.counting()));
			assertThat(studentListCaptor.getValue())
					.allSatisfy(student -> {
						long sameDepartmentPreferences = student.getPreferredCourseIds().stream()
								.filter(courseId -> student.getDepartment().equals(courseDepartments.get(courseId)))
								.count();
						long availableDepartmentCourses = coursesPerDepartment.getOrDefault(student.getDepartment(), 0L);
						assertThat(sameDepartmentPreferences)
								.isGreaterThanOrEqualTo(Math.min(availableDepartmentCourses, student.getTargetCourseLoad().longValue()));
					});
		}

		@Test
		@DisplayName("should keep generated student identities unique even when contacts repeat")
		void shouldKeepGeneratedStudentIdentitiesUniqueEvenWhenContactsRepeat() {
			when(dataGeneratorService.getRandomContactsExcluding(any(Integer.class), anyCollection()))
					.thenReturn(List.of(new Contact("Alex", "Rivera", "alex@test.edu")));
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 5, 2, 2, 9, 50, 24);

			service.generateUniversity(config);

			verify(studentRepository).saveAll(studentListCaptor.capture());
			assertThat(studentListCaptor.getValue())
					.extracting(Student::getEmail)
					.doesNotHaveDuplicates();
			assertThat(studentListCaptor.getValue())
					.extracting(Student::getStudentNumber)
					.doesNotHaveDuplicates();
		}

		@Test
		@DisplayName("should rank in-department courses ahead of electives")
		void shouldRankInDepartmentCoursesAheadOfElectives() {
			GenerationConfig config = new GenerationConfig(UniversityArchetype.COMMUNITY, 6, 2, 2, 9, 50, 48);

			service.generateUniversity(config);

			verify(courseRepository, times(48)).save(courseCaptor.capture());
			verify(studentRepository).saveAll(studentListCaptor.capture());
			Map<Long, String> courseDepartments = courseCaptor.getAllValues().stream()
					.collect(Collectors.toMap(Course::getId, Course::getDepartment, (left, right) -> left));
			Map<String, Long> coursesPerDepartment = courseCaptor.getAllValues().stream()
					.collect(Collectors.groupingBy(Course::getDepartment, Collectors.counting()));

			assertThat(studentListCaptor.getValue())
					.allSatisfy(student -> {
						long inDepartmentPrefixLength = Math.min(
								coursesPerDepartment.getOrDefault(student.getDepartment(), 0L),
								student.getTargetCourseLoad().longValue());
						List<Long> prefix = student.getPreferredCourseIds().stream()
								.limit(inDepartmentPrefixLength)
								.toList();
						assertThat(prefix)
								.allSatisfy(courseId -> assertThat(courseDepartments.get(courseId)).isEqualTo(student.getDepartment()));
					});
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
	@DisplayName("getStats")
	class GetStats {

		@Test
		@DisplayName("should include student counts and generated demand counts")
		void shouldIncludeStudentCountsAndGeneratedDemandCounts() {
			when(buildingRepository.count()).thenReturn(5L);
			when(roomRepository.count()).thenReturn(25L);
			when(instructorRepository.count()).thenReturn(12L);
			when(courseRepository.count()).thenReturn(48L);
			when(scheduleRepository.count()).thenReturn(36L);
			when(studentRepository.count()).thenReturn(300L);
			when(studentRepository.countPreferredCourseRequests()).thenReturn(1350L);

			UniversityGeneratorService.UniversityStats stats = service.getStats();

			assertThat(stats.buildings()).isEqualTo(5L);
			assertThat(stats.students()).isEqualTo(300L);
			assertThat(stats.generatedDemandCount()).isEqualTo(1350L);
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
					enrollmentRepository,
					courseRepository,
					instructorPreferenceRepository,
					instructorRepository,
					studentRepository,
					roomRepository,
					buildingRepository);

			inOrder.verify(scheduleChangeRequestRepository).deleteAll();
			inOrder.verify(scheduleRepository).deleteAll();
			inOrder.verify(enrollmentRepository).deleteAll();
			inOrder.verify(courseRepository).deleteAll();
			inOrder.verify(instructorPreferenceRepository).deleteAll();
			inOrder.verify(instructorRepository).deleteAll();
			inOrder.verify(studentRepository).deleteAll();
			inOrder.verify(roomRepository).deleteAll();
			inOrder.verify(buildingRepository).deleteAll();
		}
	}
}
