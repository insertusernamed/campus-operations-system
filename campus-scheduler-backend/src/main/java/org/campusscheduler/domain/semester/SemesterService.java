package org.campusscheduler.domain.semester;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for semester definitions.
 */
@Service
public class SemesterService {

    /**
     * Returns the canonical semester definitions used across the application.
     */
    public List<SemesterDefinitionDTO> getDefinitions() {
        return Arrays.stream(SemesterTerm.values())
                .map(this::toDTO)
                .toList();
    }

    private SemesterDefinitionDTO toDTO(SemesterTerm term) {
        return new SemesterDefinitionDTO(
                term,
                term.getDisplayName(),
                term.getStartMonth(),
                term.getStartDay(),
                term.getEndMonth(),
                term.getEndDay(),
                term.getStartYearOffset(),
                term.getEndYearOffset());
    }
}

