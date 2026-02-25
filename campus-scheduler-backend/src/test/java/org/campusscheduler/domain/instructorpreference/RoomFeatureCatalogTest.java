package org.campusscheduler.domain.instructorpreference;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomFeatureCatalogTest {

    @Test
    void optionsAreStableAndUnique() {
        List<RoomFeatureOptionResponse> options = RoomFeatureCatalog.options();

        assertThat(options).hasSize(20);
        assertThat(options)
                .extracting(RoomFeatureOptionResponse::value)
                .doesNotHaveDuplicates();
    }

    @Test
    void canonicalizeSupportsCommonAliases() {
        assertThat(RoomFeatureCatalog.canonicalize("Mic")).contains("microphone");
        assertThat(RoomFeatureCatalog.canonicalize("smart board")).contains("interactive display");
        assertThat(RoomFeatureCatalog.canonicalize("recording equipment")).contains("lecture capture");
        assertThat(RoomFeatureCatalog.canonicalize("totally unknown")).isEmpty();
    }

    @Test
    void matchingSupportsEquivalentRoomText() {
        assertThat(RoomFeatureCatalog.matchesRoomFeatures("Projector, Recording Equipment", "lecture capture"))
                .isTrue();
        assertThat(RoomFeatureCatalog.matchesRoomFeatures("Projector, Whiteboard", "fume hood"))
                .isFalse();
    }
}
