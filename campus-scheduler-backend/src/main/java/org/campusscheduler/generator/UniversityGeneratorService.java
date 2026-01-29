package org.campusscheduler.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.campusscheduler.domain.building.Building;
import org.campusscheduler.domain.building.BuildingRepository;
import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.course.CourseRepository;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.ScheduleRepository;
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
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EntityManager entityManager;

    private static final Random random = new Random();

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

    private static final String[] ROOM_TYPES = {
            "CLASSROOM", "LECTURE_HALL", "LAB", "SEMINAR", "CONFERENCE"
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
            int courses = archetype.calculateCourses(academicBuildings);
            int instructors = archetype.calculateInstructors(courses);
            int roomsPerBuilding = calculateRoomsPerBuilding(archetype, studentPopulation, academicBuildings);

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
         * Calculates rooms per building based on student density and building count.
         * Ensures enough room capacity exists for the student population.
         */
        private static int calculateRoomsPerBuilding(UniversityArchetype archetype, int students, int academicBuildings) {
            // Average class size assumption: 35 students
            // Students take ~5 courses per semester (standard full-time load)
            // Note: This is distinct from coursesPerStudent which represents catalog breadth (S/C ratio)
            // Each room can host ~8 class sessions per day (4 timeslots * 2 days overlap)
            int avgClassSize = 35;
            int avgSessionsPerRoom = 8;
            double avgCourseLoadPerStudent = 5.0; // Standard semester course load
            int totalSeatsNeeded = (int) (students * avgCourseLoadPerStudent);
            int totalRoomsNeeded = totalSeatsNeeded / (avgClassSize * avgSessionsPerRoom);
            int roomsPerBuilding = Math.max(10, totalRoomsNeeded / Math.max(1, academicBuildings));
            return Math.min(roomsPerBuilding, 30); // Cap at 30 rooms per building
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
            int timeSlots,
            String ratioInfo) {
    }

    /**
     * Clears all existing data from the database.
     */
    @Transactional
    public void clearAll() {
        log.info("Clearing all existing data...");
        scheduleRepository.deleteAll();
        courseRepository.deleteAll();
        instructorRepository.deleteAll();
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
        List<Room> rooms = generateRooms(buildings, config.roomsPerBuilding());
        List<Instructor> instructors = generateInstructors(config.instructors());
        List<Course> courses = generateCourses(instructors, config.courses());

        int timeSlots = (int) timeSlotRepository.count();

        String ratioInfo = String.format(
                "Generated using %s archetype ratios: %d students/building, %d courses/building, %.1f courses/student",
                config.archetype().getDisplayName(),
                config.archetype().getStudentsPerBuilding(),
                config.archetype().getCoursesPerBuilding(),
                config.archetype().getCoursesPerStudent()
        );

        log.info("University generation complete: {} buildings, {} rooms, {} instructors, {} courses",
                buildings.size(), rooms.size(), instructors.size(), courses.size());
        log.info(ratioInfo);

        return new GenerationResult(
                config.archetype().name(),
                config.studentPopulation(),
                buildings.size(),
                rooms.size(),
                instructors.size(),
                courses.size(),
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
            long schedules) {
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
                scheduleRepository.count());
    }

    /**
     * Generates buildings with realistic names.
     * If more buildings are requested than available names, generates additional
     * buildings with numbered suffixes (e.g., "Science Building 2").
     */
    private List<Building> generateBuildings(int count) {
        List<Building> buildings = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String baseName;
            String baseCode;
            String name;
            String code;

            if (i < BUILDING_NAMES.length) {
                name = BUILDING_NAMES[i];
                code = BUILDING_CODES[i];
            } else {
                // Generate additional buildings with numbered suffixes
                int baseIndex = i % BUILDING_NAMES.length;
                int suffix = (i / BUILDING_NAMES.length) + 1;
                baseName = BUILDING_NAMES[baseIndex];
                baseCode = BUILDING_CODES[baseIndex];
                name = baseName + " " + (suffix + 1);
                code = baseCode + (suffix + 1);
            }

            Building building = Building.builder()
                    .name(name)
                    .code(code)
                    .address((100 + i * 50) + " Campus Drive")
                    .build();
            buildings.add(buildingRepository.save(building));
        }

        log.info("Generated {} buildings", buildings.size());
        return buildings;
    }

    /**
     * Generates rooms for each building.
     */
    private List<Room> generateRooms(List<Building> buildings, int roomsPerBuilding) {
        List<Room> rooms = new ArrayList<>();

        for (Building building : buildings) {
            int baseRoomsPerFloor = roomsPerBuilding / 3;
            int remainderRooms = roomsPerBuilding % 3;

            for (int floor = 1; floor <= 3; floor++) {
                int roomsOnFloor = baseRoomsPerFloor + (floor <= remainderRooms ? 1 : 0);
                for (int r = 0; r < roomsOnFloor; r++) {
                    String type = ROOM_TYPES[random.nextInt(ROOM_TYPES.length)];
                    Room room = Room.builder()
                            .roomNumber(dataGeneratorService.generateRoomNumber(floor))
                            .capacity(dataGeneratorService.generateCapacity(type))
                            .type(Room.RoomType.valueOf(type))
                            .features(generateFeatures(type))
                            .building(building)
                            .build();
                    rooms.add(roomRepository.save(room));
                }
            }
        }

        log.info("Generated {} rooms", rooms.size());
        return rooms;
    }

    /**
     * Generates instructors from CSV contacts.
     */
    private List<Instructor> generateInstructors(int count) {
        List<Contact> contacts = dataGeneratorService.getRandomContacts(count);
        List<Instructor> instructors = new ArrayList<>();

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
            instructors.add(instructorRepository.save(instructor));
        }

        log.info("Generated {} instructors", instructors.size());
        return instructors;
    }

    /**
     * Generates courses with realistic codes and names.
     */
    private List<Course> generateCourses(List<Instructor> instructors, int count) {
        List<Course> courses = new ArrayList<>();

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
                    .enrollmentCapacity(random.nextInt(80) + 20) // 20-100 students
                    .department(prefix[1])
                    .instructor(instructor)
                    .build();
            courses.add(courseRepository.save(course));
        }

        log.info("Generated {} courses", courses.size());
        return courses;
    }

    private String generateCourseName(String department, int level) {
        String[] levelNames = { "Introduction to", "Intermediate", "Advanced", "Special Topics in" };
        return levelNames[level - 1] + " " + department;
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
}
