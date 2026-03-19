package org.campusscheduler.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final Random random = new Random();
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
     * Gets a random subset of contacts while excluding reserved identities.
     *
     * @param count number of contacts to return
     * @param excludedEmails contact emails that must not be reused
     * @return list of random contacts not present in the excluded set
     */
    public List<Contact> getRandomContactsExcluding(int count, Collection<String> excludedEmails) {
        Set<String> normalizedExcludedEmails = excludedEmails == null
                ? Set.of()
                : excludedEmails.stream()
                        .map(email -> email.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());

        List<Contact> availableContacts = loadContacts().stream()
                .filter(contact -> !normalizedExcludedEmails.contains(contact.email().toLowerCase(Locale.ROOT)))
                .toList();

        if (count >= availableContacts.size()) {
            return availableContacts;
        }

        List<Contact> shuffled = new ArrayList<>(availableContacts);
        java.util.Collections.shuffle(shuffled, random);
        return shuffled.subList(0, count);
    }

    /**
     * Generates a student-specific email that cannot collide with instructor emails.
     *
     * @param contact contact used for the base identity
     * @param sequence zero-based student sequence
     * @return unique student email
     */
    public String generateStudentEmail(Contact contact, int sequence) {
        String first = sanitizeToken(contact.firstName());
        String last = sanitizeToken(contact.lastName());
        return String.format("%s.%s.s%05d@students.campusscheduler.edu", first, last, sequence + 1);
    }

    /**
     * Generates a stable student number sequence.
     *
     * @param sequence zero-based student sequence
     * @return student number
     */
    public String generateStudentNumber(int sequence) {
        return String.format("S%08d", sequence + 1);
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
            throw new IllegalStateException("Unable to load required resource contacts.csv from classpath", e);
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
	            case "LAB" -> random.nextInt(24, 46);
	            case "SEMINAR" -> random.nextInt(12, 36);
	            case "LECTURE_HALL" -> random.nextInt(120, 281);
	            case "CONFERENCE" -> random.nextInt(12, 41);
	            default -> random.nextInt(30, 91); // CLASSROOM
	        };
	    }

    private String sanitizeToken(String value) {
        return value == null
                ? "student"
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
