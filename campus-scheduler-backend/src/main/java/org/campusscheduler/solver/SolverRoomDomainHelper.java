package org.campusscheduler.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.campusscheduler.domain.course.Course;
import org.campusscheduler.domain.room.Room;

/**
 * Builds per-course room domains to reduce search space and favor department-
 * aligned buildings.
 */
public final class SolverRoomDomainHelper {

    private static final int MAX_ALLOWED_ROOMS = 40;

    private static final Map<String, Set<String>> DEPARTMENT_BUILDING_CODES = createDepartmentBuildingCodeMap();

    private SolverRoomDomainHelper() {
    }

    public record RoomDomain(Set<String> preferredBuildingCodes, List<Room> allowedRooms) {
    }

    public static RoomDomain buildRoomDomain(Course course, List<Room> allRooms) {
        if (allRooms == null || allRooms.isEmpty()) {
            return new RoomDomain(Set.of(), List.of());
        }

        int enrollment = course != null && course.getEnrollmentCapacity() != null
                ? course.getEnrollmentCapacity()
                : 0;
        boolean labPreferred = isLabPreferred(course);
        boolean lectureHallPreferred = isLectureHallPreferred(course);
        Set<String> preferredBuildingCodes = preferredBuildingCodes(course);

        List<Room> baseCandidates = allRooms.stream()
                .filter(room -> room.getCapacity() != null && room.getCapacity() >= enrollment)
                .toList();

        if (baseCandidates.isEmpty()) {
            baseCandidates = new ArrayList<>(allRooms);
        }

        if (labPreferred) {
            List<Room> labCandidates = baseCandidates.stream()
                    .filter(room -> room.getType() == Room.RoomType.LAB)
                    .toList();
            if (!labCandidates.isEmpty()) {
                baseCandidates = labCandidates;
            }
        }

        Comparator<Room> comparator = Comparator
                .comparingInt((Room room) -> isPreferredBuilding(room, preferredBuildingCodes) ? 0 : 1)
                .thenComparingInt(room -> roomTypePenalty(room, labPreferred, lectureHallPreferred))
                .thenComparingInt(room -> capacitySlack(room, enrollment))
                .thenComparing(Room::getCapacity, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Room::getRoomNumber, Comparator.nullsLast(String::compareTo))
                .thenComparing(Room::getId, Comparator.nullsLast(Long::compareTo));

        List<Room> rankedRooms = baseCandidates.stream()
                .sorted(comparator)
                .toList();

        int candidateLimit = Math.min(MAX_ALLOWED_ROOMS, rankedRooms.size());
        List<Room> allowedRooms = candidateLimit > 0
                ? rankedRooms.subList(0, candidateLimit)
                : rankedRooms;

        if (allowedRooms.isEmpty()) {
            allowedRooms = List.copyOf(allRooms);
        } else {
            allowedRooms = List.copyOf(allowedRooms);
        }

        return new RoomDomain(preferredBuildingCodes, allowedRooms);
    }

    private static Set<String> preferredBuildingCodes(Course course) {
        String normalizedDepartment = normalize(course != null ? course.getDepartment() : null);
        if (normalizedDepartment.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> preferredCodes = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : DEPARTMENT_BUILDING_CODES.entrySet()) {
            if (normalizedDepartment.contains(entry.getKey())) {
                preferredCodes.addAll(entry.getValue());
            }
        }

        return preferredCodes.isEmpty() ? Set.of() : Set.copyOf(preferredCodes);
    }

    private static boolean isLabPreferred(Course course) {
        String normalizedDepartment = normalize(course != null ? course.getDepartment() : null);
        return normalizedDepartment.contains("chemistry")
                || normalizedDepartment.contains("biology")
                || normalizedDepartment.contains("physics");
    }

    private static boolean isLectureHallPreferred(Course course) {
        return course != null
                && course.getEnrollmentCapacity() != null
                && course.getEnrollmentCapacity() > 80;
    }

    private static int roomTypePenalty(Room room, boolean labPreferred, boolean lectureHallPreferred) {
        if (room == null || room.getType() == null) {
            return 2;
        }
        if (labPreferred) {
            return room.getType() == Room.RoomType.LAB ? 0 : 1;
        }
        if (lectureHallPreferred) {
            return room.getType() == Room.RoomType.LECTURE_HALL ? 0 : 1;
        }
        return 0;
    }

    private static int capacitySlack(Room room, int enrollment) {
        if (room == null || room.getCapacity() == null) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(room.getCapacity() - enrollment);
    }

    private static boolean isPreferredBuilding(Room room, Set<String> preferredBuildingCodes) {
        if (preferredBuildingCodes == null || preferredBuildingCodes.isEmpty()) {
            return false;
        }
        return preferredBuildingCodes.contains(normalizeCode(room != null ? room.getBuildingCode() : null));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Set<String>> createDepartmentBuildingCodeMap() {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        map.put("computer science", Set.of("CSC", "TEC", "ENG", "SCI"));
        map.put("mathematics", Set.of("MTH", "SCI"));
        map.put("physics", Set.of("PHY", "SCI", "CHM"));
        map.put("chemistry", Set.of("CHM", "SCI", "PHY"));
        map.put("biology", Set.of("BIO", "SCI", "CHM"));
        map.put("engineering", Set.of("ENG", "TEC", "SCI"));
        map.put("business", Set.of("BUS"));
        map.put("art", Set.of("ART", "MUS"));
        map.put("music", Set.of("MUS", "ART"));
        map.put("history", Set.of("SSC", "LIB"));
        map.put("english", Set.of("SSC", "LIB"));
        map.put("psychology", Set.of("PSY", "SSC"));
        return Map.copyOf(map);
    }
}
