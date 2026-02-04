package org.campusscheduler.domain.changerequest;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Response DTO for change request validation.
 */
@Data
@Builder
public class ChangeRequestValidationResponse {

    private boolean green;
    private List<String> hardConflicts;
    private List<String> softWarnings;

    public static ChangeRequestValidationResponse of(List<String> hardConflicts, List<String> softWarnings) {
        List<String> hard = hardConflicts == null ? Collections.emptyList() : hardConflicts;
        List<String> soft = softWarnings == null ? Collections.emptyList() : softWarnings;
        return ChangeRequestValidationResponse.builder()
                .hardConflicts(hard)
                .softWarnings(soft)
                .green(hard.isEmpty() && soft.isEmpty())
                .build();
    }
}
