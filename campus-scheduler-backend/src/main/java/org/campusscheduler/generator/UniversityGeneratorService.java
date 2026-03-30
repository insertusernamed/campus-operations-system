package org.campusscheduler.generator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.changerequest.ScheduleChangeRequestRepository;
import org.campusscheduler.domain.enrollment.EnrollmentRepository;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.instructorpreference.InstructorPreference;
import org.campusscheduler.domain.instructorpreference.InstructorPreferenceRepository;
import org.campusscheduler.domain.instructorpreference.RoomFeatureCatalog;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.student.Student;
import org.campusscheduler.domain.student.StudentRepository;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.campusscheduler.generator.DataGeneratorService.Contact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating a complete university dataset.
 * Creates buildings, rooms, instructors, and courses for demo/presentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UniversityGeneratorService {

    private final DataGeneratorService dataGeneratorService;
    private final ScheduleChangeRequestRepository scheduleChangeRequestRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final InstructorRepository instructorRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InstructorPreferenceRepository instructorPreferenceRepository;
    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EntityManager entityManager;

    private static final Random random = new Random();
    private static final int[] GAP_MINUTE_OPTIONS = {60, 90, 120, 150};
    private static final int[] TRAVEL_BUFFER_OPTIONS = {10, 15, 20, 25};
    private static final List<String> REQUIRED_FEATURE_POOL = RoomFeatureCatalog.options().stream()
            .map(option -> option.value())
            .toList();

    private static final String[] BUILDING_NAMES = {
            "Science Building", "Engineering Hall", "Arts Center", "Business School",
            "Mathematics Building", "Chemistry Lab", "Physics Building", "Library",
            "Student Center", "Technology Center", "Health Sciences", "Education Building",
            "Computer Science Building", "Biology Research Center", "Social Sciences",
            "Law Building", "Music Hall", "Architecture Building", "Environmental Studies",
            "Kinesiology Complex", "Psychology Building", "Economics Building",
            "Nursing Building", "Pharmacy Building", "Dentistry Building", "Medical Sciences",
            "Veterinary Building", "Agriculture Sciences", "Earth Sciences", "Astronomy Tower",
            "Media Arts Center", "Theatre Building", "Dance Studio", "Fine Arts Gallery",
            "International Studies", "Political Science", "History Building", "Philosophy Hall",
            "Linguistics Center", "Anthropology Building", "Sociology Building", "Geography Building",
            "Statistics Building", "Applied Sciences", "Nanotechnology Lab", "Robotics Center",
            "AI Research Building", "Data Science Center", "Quantum Computing Lab", "Innovation Hub"
    };

    private static final String[] BUILDING_CODES = {
            "SCI", "ENG", "ART", "BUS", "MTH", "CHM", "PHY", "LIB", "STU", "TEC", "HLT", "EDU",
            "CSC", "BIO", "SSC", "LAW", "MUS", "ARC", "ENV", "KIN", "PSY", "ECO",
            "NUR", "PHA", "DEN", "MED", "VET", "AGR", "EAR", "AST",
            "MAC", "THR", "DAN", "FAG", "INT", "POL", "HIS", "PHI",
            "LIN", "ANT", "SOC", "GEO", "STA", "APS", "NAN", "ROB",
            "AIR", "DSC", "QCL", "INH"
    };

    private static final String[] DEPARTMENTS = {
            "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology",
            "Engineering", "Business", "Art", "Music", "History", "English", "Psychology"
    };

	    private static final String[][] COURSE_PREFIXES = {
	            { "CS", "Computer Science" }, { "MATH", "Mathematics" }, { "PHYS", "Physics" },
            { "CHEM", "Chemistry" }, { "BIO", "Biology" }, { "ENG", "Engineering" },
            { "BUS", "Business" }, { "ART", "Art" }, { "MUS", "Music" }, { "HIST", "History" },
            { "ENGL", "English" }, { "PSYC", "Psychology" }
    };

    /**
     * Configuration for university generation.
     *
     * Can be created directly with explicit values, or derived from a student population
     * using research-backed ratios via the archetype-based factory methods.
     */
	    public record GenerationConfig(
	            UniversityArchetype archetype,
	            int studentPopulation,
	            int buildings,
	            int academicBuildings,
	            int roomsPerBuilding,
	            int instructors,
	            int courses) {

	        private static final double AVERAGE_COURSE_LOAD = 5.0;
	        private static final int ASSUMED_WEEKLY_TIMESLOTS = 30;

        /**
         * Creates a configuration from student population using research-based ratios.
         * This is the recommended way to generate realistic university data.
         *
         * @param archetype the university archetype (determines ratios)
         * @param studentPopulation the target student population
         * @return a configuration with derived values
         */
	        public static GenerationConfig fromStudentPopulation(UniversityArchetype archetype, int studentPopulation) {
	            int totalBuildings = archetype.calculateBuildings(studentPopulation);
	            int academicBuildings = archetype.calculateAcademicBuildings(totalBuildings);
	            int courses = calculateActiveCourses(archetype, studentPopulation, totalBuildings);
	            int instructors = archetype.calculateInstructors(courses);
	            int roomsPerBuilding = calculateRoomsPerBuilding(archetype, courses, academicBuildings);

	            return new GenerationConfig(
	                    archetype,
	                    studentPopulation,
                    totalBuildings,
                    academicBuildings,
                    roomsPerBuilding,
                    instructors,
                    courses
            );
        }

	        /**
	         * Calculates actively scheduled course sections by blending:
	         * 1) Demand from student load and expected class sizes
	         * 2) A percentage of the archetype's catalog breadth estimate
	         */
	        private static int calculateActiveCourses(
	                UniversityArchetype archetype,
	                int studentPopulation,
	                int totalBuildings) {
	            int catalogEstimate = Math.max(1, archetype.getCoursesPerBuilding() * totalBuildings);
	            int demandDriven = (int) Math.ceil((studentPopulation * AVERAGE_COURSE_LOAD) / expectedClassSize(archetype));
	            int activeCatalogPortion = (int) Math.ceil(catalogEstimate * activeCatalogFraction(archetype));
	            return Math.max(1, Math.max(demandDriven, activeCatalogPortion));
	        }

	        /**
	         * Calculates rooms per academic building from target utilization.
	         */
	        private static int calculateRoomsPerBuilding(UniversityArchetype archetype, int courses, int academicBuildings) {
	            int roomsNeeded = (int) Math.ceil(courses / (ASSUMED_WEEKLY_TIMESLOTS * targetUtilization(archetype)));
	            int roomsPerBuilding = (int) Math.ceil((double) roomsNeeded / Math.max(1, academicBuildings));
	            return Math.max(3, Math.min(roomsPerBuilding, 20));
	        }

	        private static double expectedClassSize(UniversityArchetype archetype) {
	            return switch (archetype) {
	                case METROPOLIS -> 60.0;
	                case CAMPUS_SPRAWL -> 42.0;
	                case COMMUNITY -> 30.0;
	            };
	        }

	        private static double activeCatalogFraction(UniversityArchetype archetype) {
	            return switch (archetype) {
	                case METROPOLIS -> 0.55;
	                case CAMPUS_SPRAWL -> 0.45;
	                case COMMUNITY -> 0.50;
	            };
	        }

	        private static double targetUtilization(UniversityArchetype archetype) {
	            return switch (archetype) {
	                case METROPOLIS -> 0.78;
	                case CAMPUS_SPRAWL -> 0.58;
	                case COMMUNITY -> 0.68;
	            };
	        }

        /**
         * Default configuration using COMMUNITY archetype with 8,000 students.
         * Based on Lakehead University's balanced ratios.
         */
        public static GenerationConfig defaultConfig() {
            return fromStudentPopulation(UniversityArchetype.COMMUNITY, 8000);
        }

        /**
         * Small configuration using COMMUNITY archetype with 5,000 students.
         * Represents a small comprehensive university.
         */
        public static GenerationConfig small() {
            return fromStudentPopulation(UniversityArchetype.COMMUNITY, 5000);
        }

        /**
         * Large configuration using METROPOLIS archetype with 50,000 students.
         * Represents a large research-intensive urban university.
         */
        public static GenerationConfig large() {
            return fromStudentPopulation(UniversityArchetype.METROPOLIS, 50000);
        }

        /**
         * Research campus configuration with 40,000 students.
         * Represents a sprawling research university like UBC.
         */
        public static GenerationConfig researchCampus() {
            return fromStudentPopulation(UniversityArchetype.CAMPUS_SPRAWL, 40000);
        }

        /**
         * Legacy constructor for backwards compatibility.
         * Creates a COMMUNITY archetype config from explicit values.
         */
        public static GenerationConfig legacy(int buildings, int roomsPerBuilding, int instructors, int courses) {
            return new GenerationConfig(
                    UniversityArchetype.COMMUNITY,
                    buildings * 200, // Estimate student population
                    buildings,
                    buildings, // Assume all buildings are academic
                    roomsPerBuilding,
                    instructors,
                    courses
            );
        }
    }

    /**
     * Result of university generation.
     */
    public record GenerationResult(
            String archetype,
            int studentPopulation,
            int buildings,
            int rooms,
            int instructors,
            int courses,
            int students,
            long generatedDemandCount,
            int timeSlots,
            String ratioInfo) {
    }

    /**
     * Clears all existing data from the database.
     */
    @Transactional
    public void clearAll() {
        log.info("Clearing all existing data...");
        scheduleChangeRequestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        scheduleRepository.deleteAll();
        courseRepository.deleteAll();
        instructorPreferenceRepository.deleteAll();
        instructorRepository.deleteAll();
        studentRepository.deleteAll();
        roomRepository.deleteAll();
        buildingRepository.deleteAll();
        // Flush to ensure deletes are executed before inserts
        entityManager.flush();
        log.info("All data cleared");
    }

    /**
     * Generates a complete university dataset.
     *
     * @param config generation configuration
     * @return result with counts of generated entities
     */
    @Transactional
    public GenerationResult generateUniversity(GenerationConfig config) {
        log.info("Starting university generation with config: {}", config);
        log.info("Archetype: {} - {}", config.archetype().getDisplayName(), config.archetype().getDescription());

        clearAll();

        List<Building> buildings = generateBuildings(config.academicBuildings());
        List<Room> rooms = generateRooms(buildings, config.roomsPerBuilding(), config.archetype());
        List<Instructor> instructors = generateInstructors(config.instructors());
        generateInstructorPreferences(instructors, buildings);
        List<Course> courses = generateCourses(instructors, config.courses());
        List<Student> students = generateStudents(config.studentPopulation(), instructors, courses);
        long generatedDemandCount = students.stream()
                .map(Student::getPreferredCourseIds)
                .filter(preferences -> preferences != null)
                .mapToLong(List::size)
                .sum();

        int timeSlots = (int) timeSlotRepository.count();

        String ratioInfo = String.format(
                "Generated using %s archetype ratios: %d students/building, %d courses/building, %.1f students/course",
                config.archetype().getDisplayName(),
                config.archetype().getStudentsPerBuilding(),
                config.archetype().getCoursesPerBuilding(),
                config.archetype().getStudentsPerCourse()
        );

        log.info("University generation complete: {} buildings, {} rooms, {} instructors, {} courses, {} students",
                buildings.size(), rooms.size(), instructors.size(), courses.size(), students.size());
        log.info(ratioInfo);

        return new GenerationResult(
                config.archetype().name(),
                config.studentPopulation(),
                buildings.size(),
                rooms.size(),
                instructors.size(),
                courses.size(),
                students.size(),
                generatedDemandCount,
                timeSlots,
                ratioInfo);
    }

    /**
     * Statistics about the generated university.
     */
    public record UniversityStats(
            long buildings,
            long rooms,
            long instructors,
            long courses,
            long schedules,
            long students,
            long generatedDemandCount) {
    }

    /**
     * Get current statistics of the university data.
     */
    public UniversityStats getStats() {
        return new UniversityStats(
                buildingRepository.count(),
                roomRepository.count(),
                instructorRepository.count(),
                courseRepository.count(),
                scheduleRepository.count(),
                studentRepository.count(),
                studentRepository.countPreferredCourseRequests());
    }

    /**
     * Generates buildings with realistic names.
     * If more buildings are requested than available names, generates additional
     * buildings with numbered suffixes (e.g., "Science Building 2").
     */
    private List<Building> generateBuildings(int count) {
        List<Building> buildings = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String name;
            String code;

            if (i < BUILDING_NAMES.length) {
                name = BUILDING_NAMES[i];
                code = BUILDING_CODES[i];
            } else {
                // Generate additional buildings with numbered suffixes
                int baseIndex = i % BUILDING_NAMES.length;
                int suffix = (i / BUILDING_NAMES.length) + 1;
                name = BUILDING_NAMES[baseIndex] + " " + suffix;
                code = BUILDING_CODES[baseIndex] + suffix;
            }

            Building building = Building.builder()
                    .name(name)
                    .code(code)
                    .address((100 + i * 50) + " Campus Drive")
                    .build();
            buildings.add(building);
        }

        List<Building> savedBuildings = buildingRepository.saveAll(buildings);
        log.info("Generated {} buildings", savedBuildings.size());
        return savedBuildings;
    }

    /**
     * Generates rooms for each building.
     */
	    private List<Room> generateRooms(List<Building> buildings, int roomsPerBuilding, UniversityArchetype archetype) {
	        List<Room> rooms = new ArrayList<>(buildings.size() * roomsPerBuilding);

	        for (Building building : buildings) {
	            int baseRoomsPerFloor = roomsPerBuilding / 3;
	            int remainderRooms = roomsPerBuilding % 3;
	            int roomIndexInBuilding = 0;

	            for (int floor = 1; floor <= 3; floor++) {
	                int roomsOnFloor = baseRoomsPerFloor + (floor <= remainderRooms ? 1 : 0);
	                for (int r = 0; r < roomsOnFloor; r++) {
	                    String type = pickRoomType(archetype, roomIndexInBuilding);
	                    Room room = Room.builder()
	                            .roomNumber(dataGeneratorService.generateRoomNumber(floor))
	                            .capacity(dataGeneratorService.generateCapacity(type))
	                            .type(Room.RoomType.valueOf(type))
	                            .features(generateFeatures(type))
	                            .building(building)
	                            .build();
	                    rooms.add(room);
	                    roomIndexInBuilding++;
	                }
	            }
	        }

        List<Room> savedRooms = roomRepository.saveAll(rooms);
        log.info("Generated {} rooms", savedRooms.size());
        return savedRooms;
    }

    /**
     * Generates instructors from CSV contacts.
     */
    private List<Instructor> generateInstructors(int count) {
        List<Contact> contacts = dataGeneratorService.getRandomContacts(count);
        List<Instructor> instructors = new ArrayList<>(contacts.size());

        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            String department = DEPARTMENTS[i % DEPARTMENTS.length];

            Instructor instructor = Instructor.builder()
                    .firstName(contact.firstName())
                    .lastName(contact.lastName())
                    .email(contact.email().toLowerCase())
                    .department(department)
                    .officeNumber(BUILDING_CODES[i % BUILDING_CODES.length] + "-" + (300 + i % 50))
                    .build();
            instructors.add(instructor);
        }

        List<Instructor> savedInstructors = instructorRepository.saveAll(instructors);
        log.info("Generated {} instructors", savedInstructors.size());
        return savedInstructors;
    }

    /**
     * Generates students from contacts not already used by instructors.
     */
    private List<Student> generateStudents(int count, List<Instructor> instructors, List<Course> courses) {
        List<String> reservedInstructorEmails = instructors.stream()
                .map(Instructor::getEmail)
                .toList();
        List<Contact> availableContacts = dataGeneratorService.getRandomContactsExcluding(count, reservedInstructorEmails);
        Map<String, List<Course>> coursesByDepartment = groupCoursesByDepartment(courses);
        CoursePreferenceCatalog preferenceCatalog = buildCoursePreferenceCatalog(courses, coursesByDepartment);

        if (availableContacts.isEmpty()) {
            throw new IllegalStateException("Unable to generate students because no unused contacts remain");
        }
        if (courses.isEmpty()) {
            throw new IllegalStateException("Unable to generate students because no courses were generated");
        }

        List<Student> students = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Contact contact = availableContacts.get(i % availableContacts.size());
            String department = pickStudentDepartment(courses, i);
            int yearLevel = generateYearLevel();
            int targetCourseLoad = generateTargetCourseLoad(yearLevel);
            int preferenceSeed = buildPreferenceSeed(i, department, yearLevel);
            students.add(Student.builder()
                    .studentNumber(dataGeneratorService.generateStudentNumber(i))
                    .firstName(contact.firstName())
                    .lastName(contact.lastName())
                    .email(dataGeneratorService.generateStudentEmail(contact, i))
                    .department(department)
                    .yearLevel(yearLevel)
                    .targetCourseLoad(targetCourseLoad)
                    .preferredCourseIds(generatePreferredCourseIds(
                            preferenceCatalog,
                            department,
                            yearLevel,
                            targetCourseLoad,
                            preferenceSeed))
                    .build());
        }

        List<Student> savedStudents = studentRepository.saveAll(students);
        log.info("Generated {} students", savedStudents.size());
        return savedStudents;
    }

    private Map<String, List<Course>> groupCoursesByDepartment(List<Course> courses) {
        Map<String, List<Course>> grouped = new HashMap<>();
        for (Course course : courses) {
            String department = course.getDepartment() != null ? course.getDepartment() : "General Studies";
            grouped.computeIfAbsent(department, ignored -> new ArrayList<>()).add(course);
        }
        return grouped;
    }

    private String pickStudentDepartment(List<Course> courses, int studentIndex) {
        return courses.get(studentIndex % courses.size()).getDepartment();
    }

    private int generateYearLevel() {
        double roll = random.nextDouble();
        if (roll < 0.34) {
            return 1;
        }
        if (roll < 0.62) {
            return 2;
        }
        if (roll < 0.84) {
            return 3;
        }
        return 4;
    }

    private int generateTargetCourseLoad(int yearLevel) {
        return switch (yearLevel) {
            case 1 -> random.nextDouble() < 0.7 ? 5 : 4;
            case 2 -> random.nextDouble() < 0.55 ? 5 : 4;
            case 3 -> random.nextDouble() < 0.45 ? 4 : 3;
            default -> random.nextDouble() < 0.65 ? 3 : 4;
        };
    }

    private int buildPreferenceSeed(int studentIndex, String department, int yearLevel) {
        int departmentHash = department == null ? 0 : department.hashCode();
        return 31 * (studentIndex + 1) + 17 * yearLevel + departmentHash;
    }

    private CoursePreferenceCatalog buildCoursePreferenceCatalog(
            List<Course> allCourses,
            Map<String, List<Course>> coursesByDepartment) {
        Map<Integer, List<Course>> allCoursesByLevel = groupCoursesByLevel(allCourses);
        Map<String, Map<Integer, List<Course>>> departmentCoursesByLevel = new HashMap<>();
        Map<String, Integer> departmentCourseCounts = new HashMap<>();

        for (Map.Entry<String, List<Course>> entry : coursesByDepartment.entrySet()) {
            departmentCoursesByLevel.put(entry.getKey(), groupCoursesByLevel(entry.getValue()));
            departmentCourseCounts.put(entry.getKey(), entry.getValue().size());
        }

        return new CoursePreferenceCatalog(allCoursesByLevel, departmentCoursesByLevel, departmentCourseCounts);
    }

    private Map<Integer, List<Course>> groupCoursesByLevel(List<Course> courses) {
        Map<Integer, List<Course>> grouped = new HashMap<>();
        for (Course course : courses) {
            int level = normalizeCourseLevel(extractCourseLevel(course));
            grouped.computeIfAbsent(level, ignored -> new ArrayList<>()).add(course);
        }
        grouped.values().forEach(levelCourses ->
                levelCourses.sort(Comparator.comparing(Course::getCode, Comparator.nullsLast(String::compareTo))));
        return grouped;
    }

    private List<Long> generatePreferredCourseIds(
            CoursePreferenceCatalog preferenceCatalog,
            String department,
            int yearLevel,
            int targetCourseLoad,
            int preferenceSeed) {
        int basketSize = Math.min(
                preferenceCatalog.totalCourseCount(),
                Math.max(targetCourseLoad + 3, targetCourseLoad * 2));
        if (basketSize == 0) {
            return List.of();
        }

        LinkedHashSet<Long> rankedCourseIds = new LinkedHashSet<>(basketSize);
        int inDepartmentTarget = Math.min(
                preferenceCatalog.departmentCourseCount(department),
                Math.max(targetCourseLoad, (int) Math.ceil(basketSize * 0.7)));

        addPreferredCourses(
                rankedCourseIds,
                preferenceCatalog.departmentCoursesByLevel(department),
                yearLevel,
                inDepartmentTarget,
                preferenceSeed);
        addPreferredCourses(
                rankedCourseIds,
                preferenceCatalog.allCoursesByLevel(),
                yearLevel,
                basketSize - rankedCourseIds.size(),
                mix32(preferenceSeed ^ 0x7f4a7c15));

        return new ArrayList<>(rankedCourseIds);
    }

    private void addPreferredCourses(
            LinkedHashSet<Long> rankedCourseIds,
            Map<Integer, List<Course>> coursesByLevel,
            int yearLevel,
            int targetCount,
            int preferenceSeed) {
        int initialSize = rankedCourseIds.size();
        if (targetCount <= 0 || coursesByLevel.isEmpty()) {
            return;
        }

        for (int level : courseLevelPriority(yearLevel)) {
            addRotatedCourses(
                    rankedCourseIds,
                    coursesByLevel.getOrDefault(level, List.of()),
                    targetCount,
                    initialSize,
                    mix32(preferenceSeed + (level * 97)));
            if ((rankedCourseIds.size() - initialSize) >= targetCount) {
                return;
            }
        }
    }

    private void addRotatedCourses(
            LinkedHashSet<Long> rankedCourseIds,
            List<Course> courses,
            int targetCount,
            int initialSize,
            int rotationSeed) {
        if (courses.isEmpty()) {
            return;
        }

        int startIndex = Math.floorMod(rotationSeed, courses.size());
        for (int offset = 0; offset < courses.size(); offset++) {
            if ((rankedCourseIds.size() - initialSize) >= targetCount) {
                return;
            }
            Course course = courses.get((startIndex + offset) % courses.size());
            rankedCourseIds.add(course.getId());
        }
    }

    private int extractCourseLevel(Course course) {
        if (course.getCode() == null) {
            return 1;
        }

        String digits = course.getCode().replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return 1;
        }

        int firstDigit = Character.getNumericValue(digits.charAt(0));
        return firstDigit >= 1 && firstDigit <= 8 ? firstDigit : 1;
    }

    private int normalizeCourseLevel(int yearLevel) {
        return Math.max(1, Math.min(4, yearLevel));
    }

    private int[] courseLevelPriority(int yearLevel) {
        return switch (normalizeCourseLevel(yearLevel)) {
            case 1 -> new int[]{1, 2, 3, 4};
            case 2 -> new int[]{2, 1, 3, 4};
            case 3 -> new int[]{3, 2, 4, 1};
            default -> new int[]{4, 3, 2, 1};
        };
    }

    private int mix32(int value) {
        int mixed = value;
        mixed ^= (mixed >>> 16);
        mixed *= 0x7feb352d;
        mixed ^= (mixed >>> 15);
        mixed *= 0x846ca68b;
        mixed ^= (mixed >>> 16);
        return mixed;
    }

    /**
     * Generates a baseline preference profile for each instructor.
     * This ensures friction analysis and ranked suggestions use seeded data.
     */
    private void generateInstructorPreferences(List<Instructor> instructors, List<Building> buildings) {
        List<Long> buildingIds = buildings.stream()
                .map(Building::getId)
                .toList();
        LocalDateTime generatedAt = LocalDateTime.now();

        List<InstructorPreference> preferences = new ArrayList<>(instructors.size());
        for (Instructor instructor : instructors) {
            LocalTime preferredStart = LocalTime.of(8 + random.nextInt(3), 0);
            LocalTime preferredEnd = preferredStart.plusHours(7 + random.nextInt(3));

            LinkedHashSet<Long> preferredBuildings = new LinkedHashSet<>();
            int preferredBuildingCount = buildingIds.isEmpty() ? 0 : random.nextInt(Math.min(3, buildingIds.size() + 1));
            for (int i = 0; i < preferredBuildingCount; i++) {
                preferredBuildings.add(buildingIds.get(random.nextInt(buildingIds.size())));
            }

            LinkedHashSet<String> requiredFeatures = new LinkedHashSet<>();
            int requiredFeatureCount = random.nextInt(3);
            for (int i = 0; i < requiredFeatureCount; i++) {
                requiredFeatures.add(REQUIRED_FEATURE_POOL.get(random.nextInt(REQUIRED_FEATURE_POOL.size())));
            }

            preferences.add(InstructorPreference.builder()
                    .instructor(instructor)
                    .preferredStartTime(preferredStart)
                    .preferredEndTime(preferredEnd)
                    .maxGapMinutes(GAP_MINUTE_OPTIONS[random.nextInt(GAP_MINUTE_OPTIONS.length)])
                    .minTravelBufferMinutes(TRAVEL_BUFFER_OPTIONS[random.nextInt(TRAVEL_BUFFER_OPTIONS.length)])
                    .avoidBuildingHops(random.nextDouble() < 0.75)
                    .preferredBuildingIds(preferredBuildings)
                    .requiredRoomFeatures(requiredFeatures)
                    .updatedAt(generatedAt)
                    .build());
        }

        instructorPreferenceRepository.saveAll(preferences);
        log.info("Generated {} instructor preference profiles", preferences.size());
    }

    /**
     * Generates courses with realistic codes and names.
     */
	    private List<Course> generateCourses(List<Instructor> instructors, int count) {
	        List<Course> courses = new ArrayList<>(count);

	        for (int i = 0; i < count; i++) {
            String[] prefix = COURSE_PREFIXES[i % COURSE_PREFIXES.length];
            int level = (i / COURSE_PREFIXES.length) % 4 + 1; // 1-4
            // Use unique sequence number to avoid duplicate codes
            int courseNum = level * 1000 + i;

            Instructor instructor = instructors.get(i % instructors.size());

	            Course course = Course.builder()
	                    .code(prefix[0] + courseNum)
	                    .name(generateCourseName(prefix[1], level))
	                    .description("Course in " + prefix[1] + " at level " + level)
	                    .credits(random.nextInt(3) + 2) // 2-4 credits
	                    .enrollmentCapacity(generateEnrollmentCapacity(level))
	                    .department(prefix[1])
	                    .instructor(instructor)
	                    .build();
	            courses.add(course);
        }

        List<Course> savedCourses = courseRepository.saveAll(courses);
        log.info("Generated {} courses", savedCourses.size());
        return savedCourses;
    }

	    private String generateCourseName(String department, int level) {
	        String[] levelNames = { "Introduction to", "Intermediate", "Advanced", "Special Topics in" };
	        return levelNames[level - 1] + " " + department;
	    }

	    private int generateEnrollmentCapacity(int level) {
	        return switch (level) {
	            case 1 -> random.nextInt(60, 181);
	            case 2 -> random.nextInt(35, 91);
	            case 3 -> random.nextInt(20, 56);
	            default -> random.nextInt(10, 36);
	        };
	    }

	    private String pickRoomType(UniversityArchetype archetype, int roomIndexInBuilding) {
	        // Keep a baseline instructional mix in every building.
	        if (roomIndexInBuilding == 0) {
	            return "CLASSROOM";
	        }
	        if (roomIndexInBuilding == 1) {
	            return "LAB";
	        }
	        if (roomIndexInBuilding == 2) {
	            return "LECTURE_HALL";
	        }

	        double roll = random.nextDouble();
	        return switch (archetype) {
	            case METROPOLIS -> weightedRoomType(
	                    roll,
	                    0.50, // classroom
	                    0.22, // lecture hall
	                    0.16, // lab
	                    0.09 // seminar
	            );
	            case CAMPUS_SPRAWL -> weightedRoomType(
	                    roll,
	                    0.38,
	                    0.14,
	                    0.28,
	                    0.15
	            );
	            case COMMUNITY -> weightedRoomType(
	                    roll,
	                    0.45,
	                    0.15,
	                    0.18,
	                    0.17
	            );
	        };
	    }

	    private String weightedRoomType(
	            double roll,
	            double classroomShare,
	            double lectureHallShare,
	            double labShare,
	            double seminarShare) {
	        double thresholdClassroom = classroomShare;
	        double thresholdLecture = thresholdClassroom + lectureHallShare;
	        double thresholdLab = thresholdLecture + labShare;
	        double thresholdSeminar = thresholdLab + seminarShare;

	        if (roll < thresholdClassroom) {
	            return "CLASSROOM";
	        }
	        if (roll < thresholdLecture) {
	            return "LECTURE_HALL";
	        }
	        if (roll < thresholdLab) {
	            return "LAB";
	        }
	        if (roll < thresholdSeminar) {
	            return "SEMINAR";
	        }
	        return "CONFERENCE";
	    }

    private String generateFeatures(String type) {
        return switch (type) {
            case "LAB" -> "Computers, Lab Equipment, Projector";
            case "LECTURE_HALL" -> "Projector, Microphone, Recording Equipment";
            case "SEMINAR" -> "Whiteboard, Video Conferencing";
            case "CONFERENCE" -> "Video Conferencing, Display, Whiteboard";
            default -> "Projector, Whiteboard";
        };
    }

    private record CoursePreferenceCatalog(
            Map<Integer, List<Course>> allCoursesByLevel,
            Map<String, Map<Integer, List<Course>>> departmentCoursesByLevel,
            Map<String, Integer> departmentCourseCounts) {

        Map<Integer, List<Course>> departmentCoursesByLevel(String department) {
            return departmentCoursesByLevel.getOrDefault(department, Map.of());
        }

        int departmentCourseCount(String department) {
            return departmentCourseCounts.getOrDefault(department, 0);
        }

        int totalCourseCount() {
            return allCoursesByLevel.values().stream()
                    .mapToInt(List::size)
                    .sum();
        }
    }
}
