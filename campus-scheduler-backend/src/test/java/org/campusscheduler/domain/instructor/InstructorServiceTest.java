package org.campusscheduler.domain.instructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InstructorService.
 */
@ExtendWith(MockitoExtension.class)
class InstructorServiceTest {

    @Mock
    private InstructorRepository instructorRepository;

    @InjectMocks
    private InstructorService instructorService;

    private Instructor testInstructor;

    @BeforeEach
    void setUp() {
        testInstructor = Instructor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .email("jsmith@university.edu")
                .department("Computer Science")
                .officeNumber("CS-201")
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all instructors")
        void shouldReturnAllInstructors() {
            Instructor instructor2 = Instructor.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Doe")
                    .email("jdoe@university.edu")
                    .department("Mathematics")
                    .build();

            when(instructorRepository.findAll()).thenReturn(List.of(testInstructor, instructor2));

            List<Instructor> result = instructorService.findAll();

            assertThat(result).hasSize(2);
            verify(instructorRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no instructors exist")
        void shouldReturnEmptyListWhenNoInstructorsExist() {
            when(instructorRepository.findAll()).thenReturn(List.of());

            List<Instructor> result = instructorService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return instructor when found")
        void shouldReturnInstructorWhenFound() {
            when(instructorRepository.findById(1L)).thenReturn(Optional.of(testInstructor));

            Optional<Instructor> result = instructorService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("jsmith@university.edu");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(instructorRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Instructor> result = instructorService.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDepartment")
    class FindByDepartment {

        @Test
        @DisplayName("should return instructors in department")
        void shouldReturnInstructorsInDepartment() {
            when(instructorRepository.findByDepartment("Computer Science"))
                    .thenReturn(List.of(testInstructor));

            List<Instructor> result = instructorService.findByDepartment("Computer Science");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment()).isEqualTo("Computer Science");
        }

        @Test
        @DisplayName("should return empty list when no instructors in department")
        void shouldReturnEmptyListWhenNoInstructorsInDepartment() {
            when(instructorRepository.findByDepartment("Philosophy")).thenReturn(List.of());

            List<Instructor> result = instructorService.findByDepartment("Philosophy");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create instructor")
        void shouldCreateInstructor() {
            Instructor newInstructor = Instructor.builder()
                    .firstName("Alice")
                    .lastName("Johnson")
                    .email("ajohnson@university.edu")
                    .department("Physics")
                    .build();

            when(instructorRepository.save(any(Instructor.class))).thenAnswer(i -> {
                Instructor saved = i.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            Instructor result = instructorService.create(newInstructor);

            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getEmail()).isEqualTo("ajohnson@university.edu");
            verify(instructorRepository).save(newInstructor);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update instructor when found")
        void shouldUpdateInstructorWhenFound() {
            Instructor updated = Instructor.builder()
                    .firstName("John")
                    .lastName("Smith Jr.")
                    .email("jsmith.jr@university.edu")
                    .department("Computer Science")
                    .officeNumber("CS-301")
                    .build();

            when(instructorRepository.findById(1L)).thenReturn(Optional.of(testInstructor));
            when(instructorRepository.save(any(Instructor.class))).thenAnswer(i -> i.getArgument(0));

            Optional<Instructor> result = instructorService.update(1L, updated);

            assertThat(result).isPresent();
            assertThat(result.get().getLastName()).isEqualTo("Smith Jr.");
            assertThat(result.get().getEmail()).isEqualTo("jsmith.jr@university.edu");
            assertThat(result.get().getOfficeNumber()).isEqualTo("CS-301");
        }

        @Test
        @DisplayName("should return empty when instructor not found")
        void shouldReturnEmptyWhenInstructorNotFound() {
            when(instructorRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Instructor> result = instructorService.update(999L, testInstructor);

            assertThat(result).isEmpty();
            verify(instructorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should return true when instructor is deleted")
        void shouldReturnTrueWhenInstructorIsDeleted() {
            when(instructorRepository.existsById(1L)).thenReturn(true);

            boolean result = instructorService.delete(1L);

            assertThat(result).isTrue();
            verify(instructorRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should return false when instructor not found")
        void shouldReturnFalseWhenInstructorNotFound() {
            when(instructorRepository.existsById(999L)).thenReturn(false);

            boolean result = instructorService.delete(999L);

            assertThat(result).isFalse();
            verify(instructorRepository, never()).deleteById(any());
        }
    }
}
