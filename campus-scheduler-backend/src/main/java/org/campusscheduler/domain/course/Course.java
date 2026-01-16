package org.campusscheduler.domain.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.campusscheduler.domain.instructor.Instructor;

/**
 * Represents a course that can be scheduled in a room.
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must not exceed 20 characters")
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "Course name is required")
    @Size(max = 100, message = "Course name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(length = 500)
    private String description;

    @NotNull(message = "Credits are required")
    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 12, message = "Credits must not exceed 12")
    @Column(nullable = false)
    private Integer credits;

    @NotNull(message = "Enrollment capacity is required")
    @Min(value = 1, message = "Enrollment capacity must be at least 1")
    @Column(nullable = false)
    private Integer enrollmentCapacity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @Size(max = 50, message = "Department must not exceed 50 characters")
    @Column(length = 50)
    private String department;
}
