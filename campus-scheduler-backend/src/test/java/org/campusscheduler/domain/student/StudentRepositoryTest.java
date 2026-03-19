package org.campusscheduler.domain.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository integration tests for Student entity.
 */
@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    private Student csStudent;
    private Student mathStudent;

    @BeforeEach
    void setUp() {
        csStudent = Student.builder()
                .studentNumber("S100001")
                .firstName("Avery")
                .lastName("Nguyen")
                .email("avery.nguyen@student.university.edu")
                .department("Computer Science")
                .yearLevel(2)
                .build();
        studentRepository.save(csStudent);

        mathStudent = Student.builder()
                .studentNumber("S100002")
                .firstName("Jordan")
                .lastName("Patel")
                .email("jordan.patel@student.university.edu")
                .department("Mathematics")
                .yearLevel(3)
                .build();
        studentRepository.save(mathStudent);
    }

    @Test
    @DisplayName("should find student by student number")
    void shouldFindStudentByStudentNumber() {
        Optional<Student> result = studentRepository.findByStudentNumber("S100001");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("avery.nguyen@student.university.edu");
    }

    @Test
    @DisplayName("should find student by email")
    void shouldFindStudentByEmail() {
        Optional<Student> result = studentRepository.findByEmail("jordan.patel@student.university.edu");

        assertThat(result).isPresent();
        assertThat(result.get().getStudentNumber()).isEqualTo("S100002");
    }

    @Test
    @DisplayName("should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<Student> result = studentRepository.findByEmail("nobody@student.university.edu");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find students by department")
    void shouldFindStudentsByDepartment() {
        List<Student> result = studentRepository.findByDepartment("Computer Science");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentNumber()).isEqualTo("S100001");
    }

    @Test
    @DisplayName("should return empty when department has no students")
    void shouldReturnEmptyWhenDepartmentHasNoStudents() {
        List<Student> result = studentRepository.findByDepartment("Physics");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find student by department and year level")
    void shouldFindStudentByDepartmentAndYearLevel() {
        List<Student> result = studentRepository.findByDepartmentAndYearLevel("Computer Science", 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentNumber()).isEqualTo("S100001");
    }

    @Test
    @DisplayName("should save student with generated id")
    void shouldSaveStudentWithGeneratedId() {
        Student newStudent = Student.builder()
                .studentNumber("S100003")
                .firstName("Morgan")
                .lastName("Lee")
                .email("morgan.lee@student.university.edu")
                .department("Physics")
                .yearLevel(1)
                .build();

        Student saved = studentRepository.saveAndFlush(newStudent);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDepartment()).isEqualTo("Physics");
    }

    @Test
    @DisplayName("should persist target course load and ranked course preferences")
    void shouldPersistTargetCourseLoadAndPreferences() {
        Student studentWithPreferences = Student.builder()
                .studentNumber("S100005")
                .firstName("Quinn")
                .lastName("Davis")
                .email("quinn.davis@student.university.edu")
                .department("Computer Science")
                .yearLevel(2)
                .targetCourseLoad(5)
                .preferredCourseIds(List.of(101L, 205L, 309L, 401L, 450L))
                .build();

        Student saved = studentRepository.saveAndFlush(studentWithPreferences);

        assertThat(saved.getId()).isNotNull();
        assertThat(studentRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(student -> {
                    assertThat(student.getTargetCourseLoad()).isEqualTo(5);
                    assertThat(student.getPreferredCourseIds()).containsExactly(101L, 205L, 309L, 401L, 450L);
                });
    }

    @Test
    @DisplayName("should enforce unique student number")
    void shouldEnforceUniqueStudentNumber() {
        Student duplicateStudentNumber = Student.builder()
                .studentNumber("S100001")
                .firstName("Taylor")
                .lastName("Brooks")
                .email("taylor.brooks@student.university.edu")
                .department("Computer Science")
                .yearLevel(4)
                .build();

        assertThatThrownBy(() -> studentRepository.saveAndFlush(duplicateStudentNumber))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should enforce unique email")
    void shouldEnforceUniqueEmail() {
        Student duplicateEmail = Student.builder()
                .studentNumber("S100004")
                .firstName("Reese")
                .lastName("Campbell")
                .email("avery.nguyen@student.university.edu")
                .department("Computer Science")
                .yearLevel(2)
                .build();

        assertThatThrownBy(() -> studentRepository.saveAndFlush(duplicateEmail))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should report whether student number exists")
    void shouldReportWhetherStudentNumberExists() {
        assertThat(studentRepository.existsByStudentNumber("S100001")).isTrue();
        assertThat(studentRepository.existsByStudentNumber("S199999")).isFalse();
    }

    @Test
    @DisplayName("should report whether email exists")
    void shouldReportWhetherEmailExists() {
        assertThat(studentRepository.existsByEmail("avery.nguyen@student.university.edu")).isTrue();
        assertThat(studentRepository.existsByEmail("nobody@student.university.edu")).isFalse();
    }
}
