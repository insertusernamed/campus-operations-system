package org.campusscheduler.domain.instructorpreference;

import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.instructor.Instructor;
import org.campusscheduler.domain.instructor.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for instructor preference profiles.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorPreferenceService {

    public static final LocalTime DEFAULT_PREFERRED_START = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_PREFERRED_END = LocalTime.of(18, 0);
    public static final int DEFAULT_MAX_GAP_MINUTES = 120;
    public static final int DEFAULT_MIN_TRAVEL_BUFFER_MINUTES = 15;
    public static final boolean DEFAULT_AVOID_BUILDING_HOPS = true;

    private final InstructorPreferenceRepository preferenceRepository;
    private final InstructorRepository instructorRepository;

    public Optional<InstructorPreferenceResponse> getByInstructorId(Long instructorId) {
        if (instructorRepository.findById(instructorId).isEmpty()) {
            return Optional.empty();
        }

        return preferenceRepository.findByInstructorId(instructorId)
                .map(this::toResponse)
                .or(() -> Optional.of(defaultResponse(instructorId)));
    }

    public Optional<InstructorPreferenceSettings> getEffectiveSettings(Long instructorId) {
        if (instructorRepository.findById(instructorId).isEmpty()) {
            return Optional.empty();
        }

        return preferenceRepository.findByInstructorId(instructorId)
                .map(this::toSettings)
                .or(() -> Optional.of(defaultSettings()));
    }

    @Transactional
    public Optional<InstructorPreferenceResponse> upsert(Long instructorId, InstructorPreferenceUpdateRequest request) {
        validateTimeRange(request.getPreferredStartTime(), request.getPreferredEndTime());

        Optional<Instructor> instructorOpt = instructorRepository.findById(instructorId);
        if (instructorOpt.isEmpty()) {
            return Optional.empty();
        }

        InstructorPreference preference = preferenceRepository.findByInstructorId(instructorId)
                .orElseGet(() -> InstructorPreference.builder().instructor(instructorOpt.get()).build());

        preference.setPreferredStartTime(request.getPreferredStartTime());
        preference.setPreferredEndTime(request.getPreferredEndTime());
        preference.setMaxGapMinutes(request.getMaxGapMinutes());
        preference.setMinTravelBufferMinutes(request.getMinTravelBufferMinutes());
        preference.setAvoidBuildingHops(Boolean.TRUE.equals(request.getAvoidBuildingHops()));
        preference.setPreferredBuildingIds(new LinkedHashSet<>(sanitizeBuildingIds(request.getPreferredBuildingIds())));
        preference.setRequiredRoomFeatures(new LinkedHashSet<>(sanitizeFeatures(request.getRequiredRoomFeatures())));
        preference.setUpdatedAt(LocalDateTime.now());

        InstructorPreference saved = preferenceRepository.save(preference);
        return Optional.of(toResponse(saved));
    }

    private static void validateTimeRange(LocalTime start, LocalTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("Preferred start time must be earlier than preferred end time");
        }
    }

    private static List<Long> sanitizeBuildingIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        return raw.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static List<String> sanitizeFeatures(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String feature : raw) {
            if (feature == null) {
                continue;
            }
            String trimmed = feature.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed.toLowerCase());
        }

        return new ArrayList<>(normalized);
    }

    private InstructorPreferenceResponse toResponse(InstructorPreference preference) {
        Long instructorId = preference.getInstructor() != null ? preference.getInstructor().getId() : null;
        return new InstructorPreferenceResponse(
                instructorId,
                preference.getPreferredStartTime(),
                preference.getPreferredEndTime(),
                preference.getMaxGapMinutes(),
                preference.getMinTravelBufferMinutes(),
                preference.isAvoidBuildingHops(),
                List.copyOf(preference.getPreferredBuildingIds()),
                List.copyOf(preference.getRequiredRoomFeatures()),
                preference.getUpdatedAt());
    }

    private InstructorPreferenceSettings toSettings(InstructorPreference preference) {
        return new InstructorPreferenceSettings(
                preference.getPreferredStartTime(),
                preference.getPreferredEndTime(),
                preference.getMaxGapMinutes(),
                preference.getMinTravelBufferMinutes(),
                preference.isAvoidBuildingHops(),
                List.copyOf(preference.getPreferredBuildingIds()),
                List.copyOf(preference.getRequiredRoomFeatures()));
    }

    private InstructorPreferenceSettings defaultSettings() {
        return new InstructorPreferenceSettings(
                DEFAULT_PREFERRED_START,
                DEFAULT_PREFERRED_END,
                DEFAULT_MAX_GAP_MINUTES,
                DEFAULT_MIN_TRAVEL_BUFFER_MINUTES,
                DEFAULT_AVOID_BUILDING_HOPS,
                List.of(),
                List.of());
    }

    private InstructorPreferenceResponse defaultResponse(Long instructorId) {
        InstructorPreferenceSettings defaults = defaultSettings();
        return new InstructorPreferenceResponse(
                instructorId,
                defaults.preferredStartTime(),
                defaults.preferredEndTime(),
                defaults.maxGapMinutes(),
                defaults.minTravelBufferMinutes(),
                defaults.avoidBuildingHops(),
                defaults.preferredBuildingIds(),
                defaults.requiredRoomFeatures(),
                LocalDateTime.now());
    }
}
