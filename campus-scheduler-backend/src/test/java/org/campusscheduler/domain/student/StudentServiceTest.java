package org.campusscheduler.domain.student;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StudentService.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .studentNumber("S300001")
                .firstName("Harper")
                .lastName("Chen")
                .email("harper.chen@student.university.edu")
                .department("Computer Science")
                .yearLevel(4)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all students")
        void shouldReturnAllStudents() {
            when(studentRepository.findAll()).thenReturn(List.of(student));

            List<Student> result = studentService.findAll();

            assertThat(result).containsExactly(student);
            verify(studentRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return student when found")
        void shouldReturnStudentWhenFound() {
            when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

            Optional<Student> result = studentService.findById(1L);

            assertThat(result).contains(student);
        }

        @Test
        @DisplayName("should return empty when student not found")
        void shouldReturnEmptyWhenStudentNotFound() {
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Student> result = studentService.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStudentNumber")
    class FindByStudentNumber {

        @Test
        @DisplayName("should return student when student number exists")
        void shouldReturnStudentWhenStudentNumberExists() {
            when(studentRepository.findByStudentNumber("S300001")).thenReturn(Optional.of(student));

            Optional<Student> result = studentService.findByStudentNumber("S300001");

            assertThat(result).contains(student);
        }
    }
}
