package org.campusscheduler.domain.instructorpreference;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical room feature catalog used by instructor room must-have preferences.
 *
 * Values are intentionally normalized, non-redundant, and grounded in common
 * higher-ed classroom/lab inventories.
 */
public final class RoomFeatureCatalog {

    private static final List<RoomFeatureDefinition> DEFINITIONS = List.of(
            define(
                    "projector",
                    "Projector",
                    "Presentation and AV",
                    List.of("data projector"),
                    List.of("projector", "data projector")),
            define(
                    "document camera",
                    "Document camera",
                    "Presentation and AV",
                    List.of("doc camera", "doc cam", "visualizer"),
                    List.of("document camera", "doc camera", "doc cam", "visualizer")),
            define(
                    "instructor station",
                    "Instructor station/computer",
                    "Presentation and AV",
                    List.of("instructor computer", "podium computer", "instructor workstation"),
                    List.of("instructor station", "instructor computer", "podium computer", "instructor workstation")),
            define(
                    "wireless presentation",
                    "Wireless presentation",
                    "Presentation and AV",
                    List.of("wireless projection", "wireless casting", "screen sharing"),
                    List.of("wireless presentation", "wireless projection", "wireless casting", "screen sharing")),
            define(
                    "interactive display",
                    "Interactive display/smart board",
                    "Presentation and AV",
                    List.of("smart board", "smartboard", "interactive whiteboard"),
                    List.of("interactive display", "smart board", "smartboard", "interactive whiteboard")),
            define(
                    "lecture capture",
                    "Lecture capture",
                    "Presentation and AV",
                    List.of("recording equipment", "class recording"),
                    List.of("lecture capture", "recording equipment", "class recording")),
            define(
                    "video conferencing",
                    "Video conferencing",
                    "Presentation and AV",
                    List.of("hyflex", "zoom classroom", "distance learning camera"),
                    List.of("video conferencing", "hyflex", "zoom classroom", "distance learning")),
            define(
                    "microphone",
                    "Microphone",
                    "Presentation and AV",
                    List.of("mic", "wireless mic", "podium mic", "lapel mic"),
                    List.of("microphone", "mic", "wireless mic", "podium mic", "lapel mic")),
            define(
                    "assistive listening",
                    "Assistive listening system",
                    "Presentation and AV",
                    List.of("hearing loop", "assistive listening device", "ald"),
                    List.of("assistive listening", "hearing loop", "assistive listening device", "ald")),
            define(
                    "whiteboard",
                    "Whiteboard",
                    "Teaching setup",
                    List.of("dry erase board", "marker board"),
                    List.of("whiteboard", "dry erase board", "marker board")),
            define(
                    "chalkboard",
                    "Chalkboard",
                    "Teaching setup",
                    List.of("blackboard"),
                    List.of("chalkboard", "blackboard")),
            define(
                    "movable furniture",
                    "Movable tables/chairs",
                    "Teaching setup",
                    List.of("movable tables", "movable chairs", "flexible furniture", "reconfigurable furniture"),
                    List.of("movable furniture", "movable tables", "movable chairs", "flexible furniture", "reconfigurable")),
            define(
                    "power outlets",
                    "Power outlets at seats",
                    "Teaching setup",
                    List.of("charging outlets", "outlets at seats", "in-seat power"),
                    List.of("power outlets", "charging outlets", "outlets at seats", "in-seat power")),
            define(
                    "computers",
                    "Student computers",
                    "Computing",
                    List.of("computer lab", "student workstations"),
                    List.of("computers", "computer lab", "workstations")),
            define(
                    "lab equipment",
                    "Specialized lab equipment",
                    "Lab infrastructure",
                    List.of("specialized instrumentation", "instrumentation"),
                    List.of("lab equipment", "specialized instrumentation", "instrumentation")),
            define(
                    "lab benches",
                    "Lab benches/workbenches",
                    "Lab infrastructure",
                    List.of("lab bench", "workbench", "workbenches"),
                    List.of("lab benches", "lab bench", "workbench", "workbenches")),
            define(
                    "sinks",
                    "Sinks/water access",
                    "Lab infrastructure",
                    List.of("water access", "water taps", "sink"),
                    List.of("sinks", "sink", "water access", "water taps")),
            define(
                    "gas outlets",
                    "Gas outlets",
                    "Lab infrastructure",
                    List.of("gas taps", "natural gas"),
                    List.of("gas outlets", "gas taps", "natural gas")),
            define(
                    "fume hood",
                    "Chemical fume hood",
                    "Lab safety",
                    List.of("chemical hood", "ventilation hood"),
                    List.of("fume hood", "chemical hood", "ventilation hood")),
            define(
                    "eyewash station",
                    "Eyewash and safety shower",
                    "Lab safety",
                    List.of("eyewash", "safety shower", "emergency shower"),
                    List.of("eyewash station", "eyewash", "safety shower", "emergency shower"))
    );

    private static final List<RoomFeatureOptionResponse> OPTIONS = DEFINITIONS.stream()
            .map(RoomFeatureDefinition::option)
            .toList();

    private static final Map<String, RoomFeatureDefinition> DEFINITIONS_BY_VALUE = DEFINITIONS.stream()
            .collect(LinkedHashMap::new, (map, definition) -> map.put(definition.option().value(), definition), Map::putAll);

    private static final Map<String, String> LOOKUP_BY_ALIAS = buildLookupByAlias();

    private RoomFeatureCatalog() {
    }

    public static List<RoomFeatureOptionResponse> options() {
        return OPTIONS;
    }

    public static Optional<String> canonicalize(String rawFeature) {
        String normalized = normalize(rawFeature);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LOOKUP_BY_ALIAS.get(normalized));
    }

    public static boolean matchesRoomFeatures(String roomFeatures, String requiredFeature) {
        if (roomFeatures == null || roomFeatures.isBlank()) {
            return false;
        }

        Optional<String> canonical = canonicalize(requiredFeature);
        if (canonical.isEmpty()) {
            return false;
        }

        RoomFeatureDefinition definition = DEFINITIONS_BY_VALUE.get(canonical.get());
        if (definition == null) {
            return false;
        }

        String normalizedRoomFeatures = normalize(roomFeatures);
        return definition.matchKeywords().stream()
                .anyMatch(keyword -> !keyword.isEmpty() && normalizedRoomFeatures.contains(keyword));
    }

    public static String labelFor(String featureValue) {
        Optional<String> canonical = canonicalize(featureValue);
        if (canonical.isEmpty()) {
            return featureValue;
        }

        RoomFeatureDefinition definition = DEFINITIONS_BY_VALUE.get(canonical.get());
        return definition == null ? featureValue : definition.option().label();
    }

    private static Map<String, String> buildLookupByAlias() {
        Map<String, String> lookup = new LinkedHashMap<>();

        for (RoomFeatureDefinition definition : DEFINITIONS) {
            String value = definition.option().value();

            putAlias(lookup, value, value);
            putAlias(lookup, definition.option().label(), value);

            for (String alias : definition.aliases()) {
                putAlias(lookup, alias, value);
            }
        }

        return lookup;
    }

    private static void putAlias(Map<String, String> lookup, String alias, String value) {
        String normalized = normalize(alias);
        if (!normalized.isEmpty()) {
            lookup.putIfAbsent(normalized, value);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static RoomFeatureDefinition define(
            String value,
            String label,
            String category,
            List<String> aliases,
            List<String> matchKeywords) {
        LinkedHashSet<String> normalizedKeywords = new LinkedHashSet<>();
        for (String keyword : matchKeywords) {
            String normalized = normalize(keyword);
            if (!normalized.isEmpty()) {
                normalizedKeywords.add(normalized);
            }
        }
        List<String> normalizedKeywordList = List.copyOf(normalizedKeywords);

        return new RoomFeatureDefinition(
                new RoomFeatureOptionResponse(value, label, category, normalizedKeywordList),
                aliases,
                normalizedKeywordList);
    }

    private record RoomFeatureDefinition(
            RoomFeatureOptionResponse option,
            List<String> aliases,
            List<String> matchKeywords
    ) {
    }
}
