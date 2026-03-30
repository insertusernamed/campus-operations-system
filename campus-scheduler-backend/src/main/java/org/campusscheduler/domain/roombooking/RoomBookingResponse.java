package org.campusscheduler.domain.roombooking;

import org.campusscheduler.domain.room.RoomResponse;
import org.campusscheduler.domain.schedule.ScheduleResponse;

import java.time.Instant;
import java.util.List;

/**
 * Privacy-aware response payload for room booking calendar items.
 */
public record RoomBookingResponse(
        Long id,
        RoomResponse room,
        ScheduleResponse.TimeSlotSummary timeSlot,
        String semester,
        Instant createdAt,
        int participantCount,
        boolean viewerCanSeeStudentDetails,
        boolean viewerIsOwner,
        boolean viewerIsParticipant,
        RoomBookingParticipantResponse bookedBy,
        List<RoomBookingParticipantResponse> participants) {
}
