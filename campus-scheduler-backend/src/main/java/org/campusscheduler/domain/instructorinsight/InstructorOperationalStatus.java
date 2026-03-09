package org.campusscheduler.domain.instructorinsight;

/**
 * Primary operational status used in admin queues.
 */
public enum InstructorOperationalStatus {
    COVERAGE_RISK,
    OVERLOADED,
    UNDER_UTILIZED,
    PREFERENCE_INCOMPLETE,
    FRICTION_HOTSPOT,
    READY
}
