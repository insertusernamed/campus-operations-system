package org.campusscheduler.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating realistic demo data for presentations.
 * Uses contacts.csv for realistic names and generates buildings,
 * rooms, instructors, and courses.
 */
@Service
@Slf4j
public class DataGeneratorService {

    private final Random random = new Random();
    private List<String[]> contactsCache = null;

    /**
     * Represents a contact loaded from CSV.
     */
    public record Contact(String firstName, String lastName, String email) {
    }

    /**
     * Loads contacts from the CSV file.
     * Caches the result for subsequent calls.
     *
     * @return list of contacts
     */
    public List<Contact> loadContacts() {
        if (contactsCache == null) {
            contactsCache = parseCsvFile();
        }
        return contactsCache.stream()
                .map(row -> new Contact(row[0], row[1], row[2]))
                .toList();
    }

    /**
     * Gets a random subset of contacts.
     *
     * @param count number of contacts to return
     * @return list of random contacts
     */
    public List<Contact> getRandomContacts(int count) {
        List<Contact> all = loadContacts();
        if (count >= all.size()) {
            return all;
        }

        List<Contact> shuffled = new ArrayList<>(all);
        java.util.Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }

    /**
     * Parses the contacts.csv file from classpath.
     *
     * @return list of string arrays representing CSV rows
     */
    private List<String[]> parseCsvFile() {
        List<String[]> rows = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("contacts.csv");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue; // Skip header row
                    }
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        rows.add(parts);
                    }
                }
            }
            log.info("Loaded {} contacts from CSV", rows.size());
        } catch (IOException e) {
            log.error("Failed to load contacts.csv", e);
        }
        return rows;
    }

    /**
     * Generates a random room number in realistic format.
     *
     * @param floor the floor number (1-5)
     * @return room number like "101", "215", etc.
     */
    public String generateRoomNumber(int floor) {
        int roomNum = (floor * 100) + random.nextInt(1, 30);
        return String.valueOf(roomNum);
    }

    /**
     * Generates a random room capacity based on room type.
     *
     * @param type the room type
     * @return capacity between appropriate range
     */
    public int generateCapacity(String type) {
        return switch (type) {
            case "LAB" -> random.nextInt(20, 35);
            case "SEMINAR" -> random.nextInt(10, 25);
            case "LECTURE_HALL" -> random.nextInt(100, 250);
            case "CONFERENCE" -> random.nextInt(8, 20);
            default -> random.nextInt(25, 60); // CLASSROOM
        };
    }
}
