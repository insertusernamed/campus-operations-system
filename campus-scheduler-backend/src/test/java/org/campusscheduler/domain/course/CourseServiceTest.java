package org.campusscheduler.domain.course;

import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
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
 * Unit tests for CourseService.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private Instructor testInstructor;

    @BeforeEach
    void setUp() {
        testInstructor = Instructor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .email("jsmith@university.edu")
                .department("Computer Science")
                .build();

        testCourse = Course.builder()
                .id(1L)
                .code("CS101")
                .name("Introduction to Programming")
                .description("Learn the basics of programming")
                .credits(3)
                .enrollmentCapacity(30)
                .department("Computer Science")
                .instructor(testInstructor)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all courses")
        void shouldReturnAllCourses() {
            Course course2 = Course.builder()
                    .id(2L)
                    .code("CS201")
                    .name("Data Structures")
                    .credits(3)
                    .enrollmentCapacity(25)
                    .build();

            when(courseRepository.findAll()).thenReturn(List.of(testCourse, course2));

            List<Course> result = courseService.findAll();

            assertThat(result).hasSize(2);
            verify(courseRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no courses exist")
        void shouldReturnEmptyListWhenNoCoursesExist() {
            when(courseRepository.findAll()).thenReturn(List.of());

            List<Course> result = courseService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return course when found")
        void shouldReturnCourseWhenFound() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            Optional<Course> result = courseService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("CS101");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Course> result = courseService.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("should return course when found by code")
        void shouldReturnCourseWhenFoundByCode() {
            when(courseRepository.findByCode("CS101")).thenReturn(Optional.of(testCourse));

            Optional<Course> result = courseService.findByCode("CS101");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Introduction to Programming");
        }
    }

    @Nested
    @DisplayName("findByDepartment")
    class FindByDepartment {

        @Test
        @DisplayName("should return courses in department")
        void shouldReturnCoursesInDepartment() {
            when(courseRepository.findByDepartment("Computer Science"))
                    .thenReturn(List.of(testCourse));

            List<Course> result = courseService.findByDepartment("Computer Science");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment()).isEqualTo("Computer Science");
        }
    }

    @Nested
    @DisplayName("findByInstructorId")
    class FindByInstructorId {

        @Test
        @DisplayName("should return courses taught by instructor")
        void shouldReturnCoursesTaughtByInstructor() {
            when(courseRepository.findByInstructorId(1L)).thenReturn(List.of(testCourse));

            List<Course> result = courseService.findByInstructorId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInstructor().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create course")
        void shouldCreateCourse() {
            Course newCourse = Course.builder()
                    .code("CS301")
                    .name("Algorithms")
                    .credits(4)
                    .enrollmentCapacity(20)
                    .build();

            when(courseRepository.save(any(Course.class))).thenAnswer(i -> {
                Course saved = i.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            Course result = courseService.create(newCourse);

            assertThat(result.getId()).isEqualTo(2L);
            verify(courseRepository).save(newCourse);
        }
    }

    @Nested
    @DisplayName("createWithInstructor")
    class CreateWithInstructor {

        @Test
        @DisplayName("should create course with instructor when instructor exists")
        void shouldCreateCourseWithInstructorWhenExists() {
            Course newCourse = Course.builder()
                    .code("CS301")
                    .name("Algorithms")
                    .credits(4)
                    .enrollmentCapacity(20)
                    .build();

            when(instructorRepository.findById(1L)).thenReturn(Optional.of(testInstructor));
            when(courseRepository.save(any(Course.class))).thenAnswer(i -> {
                Course saved = i.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            Optional<Course> result = courseService.createWithInstructor(newCourse, 1L);

            assertThat(result).isPresent();
            assertThat(result.get().getInstructor()).isEqualTo(testInstructor);
            verify(courseRepository).save(newCourse);
        }

        @Test
        @DisplayName("should return empty when instructor not found")
        void shouldReturnEmptyWhenInstructorNotFound() {
            Course newCourse = Course.builder()
                    .code("CS301")
                    .name("Algorithms")
                    .credits(4)
                    .enrollmentCapacity(20)
                    .build();

            when(instructorRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Course> result = courseService.createWithInstructor(newCourse, 999L);

            assertThat(result).isEmpty();
            verify(courseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update course when found")
        void shouldUpdateCourseWhenFound() {
            Course updated = Course.builder()
                    .code("CS101-A")
                    .name("Intro to Programming - Advanced")
                    .description("Updated description")
                    .credits(4)
                    .enrollmentCapacity(35)
                    .department("Computer Science")
                    .build();

            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

            Optional<Course> result = courseService.update(1L, updated);

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("CS101-A");
            assertThat(result.get().getCredits()).isEqualTo(4);
        }

        @Test
        @DisplayName("should return empty when course not found")
        void shouldReturnEmptyWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Course> result = courseService.update(999L, testCourse);

            assertThat(result).isEmpty();
            verify(courseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignInstructor")
    class AssignInstructor {

        @Test
        @DisplayName("should assign instructor when both exist")
        void shouldAssignInstructorWhenBothExist() {
            Course courseWithoutInstructor = Course.builder()
                    .id(2L)
                    .code("CS201")
                    .name("Data Structures")
                    .credits(3)
                    .enrollmentCapacity(25)
                    .build();

            when(courseRepository.findById(2L)).thenReturn(Optional.of(courseWithoutInstructor));
            when(instructorRepository.findById(1L)).thenReturn(Optional.of(testInstructor));
            when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

            Optional<Course> result = courseService.assignInstructor(2L, 1L);

            assertThat(result).isPresent();
            assertThat(result.get().getInstructor()).isEqualTo(testInstructor);
        }

        @Test
        @DisplayName("should return empty when course not found")
        void shouldReturnEmptyWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Course> result = courseService.assignInstructor(999L, 1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when instructor not found")
        void shouldReturnEmptyWhenInstructorNotFound() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(instructorRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Course> result = courseService.assignInstructor(1L, 999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should return true when course is deleted")
        void shouldReturnTrueWhenCourseIsDeleted() {
            when(courseRepository.existsById(1L)).thenReturn(true);

            boolean result = courseService.delete(1L);

            assertThat(result).isTrue();
            verify(courseRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should return false when course not found")
        void shouldReturnFalseWhenCourseNotFound() {
            when(courseRepository.existsById(999L)).thenReturn(false);

            boolean result = courseService.delete(999L);

            assertThat(result).isFalse();
            verify(courseRepository, never()).deleteById(any());
        }
    }
}
