package org.campusscheduler.domain.student;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service layer for Student read operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    /**
     * Get all students.
     *
     * @return list of all students
     */
    public List<Student> findAll() {
        return studentRepository.findAll().stream()
                .filter(Objects::nonNull)
                .peek(this::initialize)
                .toList();
    }

    /**
     * Find a student by ID.
     *
     * @param id the student ID
     * @return optional containing the student if found
     */
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    initialize(student);
                    return student;
                });
    }

    /**
     * Find a student by student number.
     *
     * @param studentNumber the student number
     * @return optional containing the student if found
     */
    public Optional<Student> findByStudentNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .map(student -> {
                    initialize(student);
                    return student;
                });
    }

    private void initialize(Student student) {
        if (student.getPreferredCourseIds() != null) {
            student.getPreferredCourseIds().size();
        }
    }
}
