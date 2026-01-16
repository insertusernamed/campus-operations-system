package org.campusscheduler.domain.course;

import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for Course entity.
 */
@DataJpaTest
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    private Instructor csInstructor;
    private Course csCourse;
    private Course mathCourse;

    @BeforeEach
    void setUp() {
        csInstructor = Instructor.builder()
                .firstName("John")
                .lastName("Smith")
                .email("jsmith@university.edu")
                .department("Computer Science")
                .build();
        instructorRepository.save(csInstructor);

        csCourse = Course.builder()
                .code("CS101")
                .name("Introduction to Programming")
                .description("Learn programming basics")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .instructor(csInstructor)
                .build();
        courseRepository.save(csCourse);

        mathCourse = Course.builder()
                .code("MATH201")
                .name("Calculus II")
                .credits(4)
                .enrollmentCapacity(25)
                .department("Mathematics")
                .build();
        courseRepository.save(mathCourse);
    }

    @Test
    @DisplayName("should find course by code")
    void shouldFindCourseByCode() {
        Optional<Course> result = courseRepository.findByCode("CS101");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Introduction to Programming");
    }

    @Test
    @DisplayName("should return empty when code not found")
    void shouldReturnEmptyWhenCodeNotFound() {
        Optional<Course> result = courseRepository.findByCode("PHYS999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find courses by department")
    void shouldFindCoursesByDepartment() {
        List<Course> result = courseRepository.findByDepartment("Computer Science");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("should return empty list when no courses in department")
    void shouldReturnEmptyListWhenNoCoursesInDepartment() {
        List<Course> result = courseRepository.findByDepartment("Physics");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find courses by instructor ID")
    void shouldFindCoursesByInstructorId() {
        List<Course> result = courseRepository.findByInstructorId(csInstructor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("should check if code exists")
    void shouldCheckIfCodeExists() {
        boolean exists = courseRepository.existsByCode("CS101");
        boolean notExists = courseRepository.existsByCode("PHYS999");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("should save course with all fields")
    void shouldSaveCourseWithAllFields() {
        Course newCourse = Course.builder()
                .code("PHYS101")
                .name("Physics I")
                .description("Introduction to physics")
                .credits(4)
                .enrollmentCapacity(40)
                .department("Physics")
                .build();

        Course saved = courseRepository.save(newCourse);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode()).isEqualTo("PHYS101");
        assertThat(saved.getCredits()).isEqualTo(4);
    }

    @Test
    @DisplayName("should save course with instructor relationship")
    void shouldSaveCourseWithInstructorRelationship() {
        Course newCourse = Course.builder()
                .code("CS201")
                .name("Data Structures")
                .credits(3)
                .enrollmentCapacity(25)
                .department("Computer Science")
                .instructor(csInstructor)
                .build();

        Course saved = courseRepository.save(newCourse);

        assertThat(saved.getInstructor()).isNotNull();
        assertThat(saved.getInstructor().getId()).isEqualTo(csInstructor.getId());
    }
}
