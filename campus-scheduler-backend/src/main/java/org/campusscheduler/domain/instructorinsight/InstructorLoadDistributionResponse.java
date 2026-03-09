package org.campusscheduler.domain.instructorinsight;

import java.util.List;

/**
 * Department load distribution view for planning.
 */
public record InstructorLoadDistributionResponse(
        String semester,
        List<InstructorDepartmentLoadResponse> departments
) {
}
