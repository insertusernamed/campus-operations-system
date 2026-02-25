package org.campusscheduler.domain.instructorpreference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for instructor preference persistence.
 */
@Repository
public interface InstructorPreferenceRepository extends JpaRepository<InstructorPreference, Long> {

    Optional<InstructorPreference> findByInstructorId(Long instructorId);
}
