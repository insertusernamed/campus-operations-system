package org.campusscheduler.domain.instructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for Instructor entity.
 */
@DataJpaTest
class InstructorRepositoryTest {

    @Autowired
    private InstructorRepository instructorRepository;

    private Instructor csInstructor;
    private Instructor mathInstructor;

    @BeforeEach
    void setUp() {
        csInstructor = Instructor.builder()
                .firstName("John")
                .lastName("Smith")
                .email("jsmith@university.edu")
                .department("Computer Science")
                .officeNumber("CS-201")
                .build();
        instructorRepository.save(csInstructor);

        mathInstructor = Instructor.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jdoe@university.edu")
                .department("Mathematics")
                .officeNumber("MATH-101")
                .build();
        instructorRepository.save(mathInstructor);
    }

    @Test
    @DisplayName("should find instructor by email")
    void shouldFindInstructorByEmail() {
        Optional<Instructor> result = instructorRepository.findByEmail("jsmith@university.edu");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
        assertThat(result.get().getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<Instructor> result = instructorRepository.findByEmail("nobody@university.edu");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find instructors by department")
    void shouldFindInstructorsByDepartment() {
        List<Instructor> result = instructorRepository.findByDepartment("Computer Science");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("jsmith@university.edu");
    }

    @Test
    @DisplayName("should return empty list when no instructors in department")
    void shouldReturnEmptyListWhenNoInstructorsInDepartment() {
        List<Instructor> result = instructorRepository.findByDepartment("Philosophy");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should check if email exists")
    void shouldCheckIfEmailExists() {
        boolean exists = instructorRepository.existsByEmail("jsmith@university.edu");
        boolean notExists = instructorRepository.existsByEmail("nobody@university.edu");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("should save instructor with all fields")
    void shouldSaveInstructorWithAllFields() {
        Instructor newInstructor = Instructor.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("ajohnson@university.edu")
                .department("Physics")
                .officeNumber("PHY-301")
                .build();

        Instructor saved = instructorRepository.save(newInstructor);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Alice");
        assertThat(saved.getOfficeNumber()).isEqualTo("PHY-301");
    }

    @Test
    @DisplayName("should find multiple instructors in same department")
    void shouldFindMultipleInstructorsInSameDepartment() {
        Instructor anotherCSInstructor = Instructor.builder()
                .firstName("Bob")
                .lastName("Wilson")
                .email("bwilson@university.edu")
                .department("Computer Science")
                .build();
        instructorRepository.save(anotherCSInstructor);

        List<Instructor> result = instructorRepository.findByDepartment("Computer Science");

        assertThat(result).hasSize(2);
    }
}
