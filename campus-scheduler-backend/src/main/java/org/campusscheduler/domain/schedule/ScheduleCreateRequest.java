package org.campusscheduler.domain.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a schedule.
 */
@Data
public class ScheduleCreateRequest {

	@NotNull(message = "Course ID is required")
	private Long courseId;

	@NotNull(message = "Room ID is required")
	private Long roomId;

	@NotNull(message = "Time slot ID is required")
	private Long timeSlotId;

	@NotBlank(message = "Semester is required")
	private String semester;
}
