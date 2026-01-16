package org.campusscheduler.domain.instructor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Instructor business logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorService {

    private final InstructorRepository instructorRepository;

    /**
     * Get all instructors.
     *
     * @return list of all instructors
     */
    public List<Instructor> findAll() {
        return instructorRepository.findAll();
    }

    /**
     * Find an instructor by ID.
     *
     * @param id the instructor ID
     * @return optional containing the instructor if found
     */
    public Optional<Instructor> findById(Long id) {
        return instructorRepository.findById(id);
    }

    /**
     * Find instructors by department.
     *
     * @param department the department name
     * @return list of instructors in the department
     */
    public List<Instructor> findByDepartment(String department) {
        return instructorRepository.findByDepartment(department);
    }

    /**
     * Create a new instructor.
     *
     * @param instructor the instructor to create
     * @return the created instructor
     */
    @Transactional
    public Instructor create(Instructor instructor) {
        String email = instructor.getEmail();
        if (email != null && instructorRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Instructor with email " + email + " already exists");
        }
        return instructorRepository.save(instructor);
    }

    /**
     * Update an existing instructor.
     *
     * @param id      the instructor ID
     * @param updated the updated instructor data
     * @return optional containing the updated instructor if found
     */
    @Transactional
    public Optional<Instructor> update(Long id, Instructor updated) {
        return instructorRepository.findById(id)
                .map(existing -> {
                    String newEmail = updated.getEmail();
                    if (newEmail != null && !newEmail.equals(existing.getEmail())) {
                        Optional<Instructor> existingWithEmail = instructorRepository.findByEmail(newEmail);
                        if (existingWithEmail.isPresent() && !existingWithEmail.get().getId().equals(id)) {
                            throw new IllegalArgumentException("Instructor with email " + newEmail + " already exists");
                        }
                    }
                    existing.setFirstName(updated.getFirstName());
                    existing.setLastName(updated.getLastName());
                    existing.setEmail(updated.getEmail());
                    existing.setDepartment(updated.getDepartment());
                    existing.setOfficeNumber(updated.getOfficeNumber());
                    return instructorRepository.save(existing);
                });
    }

    /**
     * Delete an instructor by ID.
     *
     * @param id the instructor ID
     * @return true if deleted, false if not found
     */
    @Transactional
    public boolean delete(Long id) {
        if (instructorRepository.existsById(id)) {
            instructorRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
