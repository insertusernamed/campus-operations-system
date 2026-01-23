package org.campusscheduler.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataGeneratorService.
 */
class DataGeneratorServiceTest {

    private DataGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new DataGeneratorService();
    }

    @Nested
    @DisplayName("loadContacts")
    class LoadContacts {

        @Test
        @DisplayName("should load contacts from CSV file")
        void shouldLoadContactsFromCsv() {
            List<DataGeneratorService.Contact> contacts = service.loadContacts();

            assertThat(contacts).isNotEmpty();
            assertThat(contacts.get(0).firstName()).isNotBlank();
            assertThat(contacts.get(0).lastName()).isNotBlank();
            assertThat(contacts.get(0).email()).contains("@");
        }

        @Test
        @DisplayName("should return consistent data on subsequent calls")
        void shouldReturnConsistentData() {
            List<DataGeneratorService.Contact> first = service.loadContacts();
            List<DataGeneratorService.Contact> second = service.loadContacts();

            assertThat(first).hasSameSizeAs(second);
            assertThat(first.get(0)).isEqualTo(second.get(0));
        }
    }

    @Nested
    @DisplayName("getRandomContacts")
    class GetRandomContacts {

        @Test
        @DisplayName("should return requested number of contacts")
        void shouldReturnRequestedCount() {
            List<DataGeneratorService.Contact> contacts = service.getRandomContacts(50);

            assertThat(contacts).hasSize(50);
        }

        @Test
        @DisplayName("should return all contacts when count exceeds available")
        void shouldReturnAllWhenCountExceedsAvailable() {
            List<DataGeneratorService.Contact> all = service.loadContacts();
            List<DataGeneratorService.Contact> requested = service.getRandomContacts(Integer.MAX_VALUE);

            assertThat(requested).hasSize(all.size());
        }
    }

    @Nested
    @DisplayName("generateRoomNumber")
    class GenerateRoomNumber {

        @Test
        @DisplayName("should generate room number starting with floor number")
        void shouldStartWithFloorNumber() {
            String room = service.generateRoomNumber(2);

            assertThat(room).startsWith("2");
            assertThat(Integer.parseInt(room)).isBetween(201, 229);
        }
    }

    @Nested
    @DisplayName("generateCapacity")
    class GenerateCapacity {

        @Test
        @DisplayName("should generate appropriate capacity for LAB")
        void shouldGenerateLabCapacity() {
            int capacity = service.generateCapacity("LAB");

            assertThat(capacity).isBetween(20, 34);
        }

        @Test
        @DisplayName("should generate appropriate capacity for LECTURE_HALL")
        void shouldGenerateLectureHallCapacity() {
            int capacity = service.generateCapacity("LECTURE_HALL");

            assertThat(capacity).isBetween(100, 249);
        }

        @Test
        @DisplayName("should generate classroom capacity for unknown type")
        void shouldGenerateDefaultCapacity() {
            int capacity = service.generateCapacity("CLASSROOM");

            assertThat(capacity).isBetween(25, 59);
        }
    }
}
