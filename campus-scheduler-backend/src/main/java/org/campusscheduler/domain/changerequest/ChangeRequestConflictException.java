package org.campusscheduler.domain.changerequest;

import java.util.List;

public class ChangeRequestConflictException extends RuntimeException {

    private final List<String> hardConflicts;

    public ChangeRequestConflictException(String message, List<String> hardConflicts) {
        super(message);
        this.hardConflicts = hardConflicts;
    }

    public List<String> getHardConflicts() {
        return hardConflicts;
    }
}
