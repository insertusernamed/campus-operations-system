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

    private final Random random = new Random();

    private static final String[] BUILDING_NAMES = {
            "Science Building", "Engineering Hall", "Arts Center", "Business School",
            "Mathematics Building", "Chemistry Lab", "Physics Building", "Library",
            "Student Center", "Technology Center", "Health Sciences", "Education Building"
    };

    private static final String[] BUILDING_CODES = {
            "SCI", "ENG", "ART", "BUS", "MTH", "CHM", "PHY", "LIB", "STU", "TEC", "HLT", "EDU"
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
     */
    public record GenerationConfig(
            int buildings,
            int roomsPerBuilding,
            int instructors,
            int courses) {

        public static GenerationConfig defaultConfig() {
            return new GenerationConfig(8, 15, 200, 500);
        }

        public static GenerationConfig small() {
            return new GenerationConfig(4, 10, 50, 100);
        }

        public static GenerationConfig large() {
            return new GenerationConfig(12, 20, 300, 800);
        }
    }

    /**
     * Result of university generation.
     */
    public record GenerationResult(
            int buildings,
            int rooms,
            int instructors,
            int courses,
            int timeSlots) {
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

        clearAll();

        List<Building> buildings = generateBuildings(config.buildings());
        List<Room> rooms = generateRooms(buildings, config.roomsPerBuilding());
        List<Instructor> instructors = generateInstructors(config.instructors());
        List<Course> courses = generateCourses(instructors, config.courses());

        int timeSlots = (int) timeSlotRepository.count();

        log.info("University generation complete: {} buildings, {} rooms, {} instructors, {} courses",
                buildings.size(), rooms.size(), instructors.size(), courses.size());

        return new GenerationResult(
                buildings.size(),
                rooms.size(),
                instructors.size(),
                courses.size(),
                timeSlots);
    }

    /**
     * Generates buildings with realistic names.
     */
    private List<Building> generateBuildings(int count) {
        List<Building> buildings = new ArrayList<>();
        int max = Math.min(count, BUILDING_NAMES.length);

        for (int i = 0; i < max; i++) {
            Building building = Building.builder()
                    .name(BUILDING_NAMES[i])
                    .code(BUILDING_CODES[i])
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
            for (int floor = 1; floor <= 3; floor++) {
                int roomsOnFloor = roomsPerBuilding / 3;
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
            int courseNum = level * 100 + (i % 99) + 1;

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
