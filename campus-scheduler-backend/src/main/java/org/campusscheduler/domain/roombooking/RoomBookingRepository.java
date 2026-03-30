package org.campusscheduler.domain.roombooking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;

/**
 * Repository for room booking persistence and policy checks.
 */
@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {

    @Query("""
            select distinct rb
            from RoomBooking rb
            left join rb.participants participant
            where (:semester is null or :semester = '' or rb.semester = :semester)
              and (:studentId is null or rb.bookedBy.id = :studentId or participant.id = :studentId)
            """)
    List<RoomBooking> findByFilters(@Param("semester") String semester, @Param("studentId") Long studentId);

    @Query("""
            select distinct rb
            from RoomBooking rb
            where rb.room.id = :roomId
              and rb.timeSlot.id = :timeSlotId
              and (
                    rb.bookingDate = :bookingDate
                    or (rb.bookingDate is null and rb.semester = :semester)
              )
            """)
    List<RoomBooking> findConflictingRoomBookings(
            @Param("roomId") Long roomId,
            @Param("timeSlotId") Long timeSlotId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("semester") String semester);

    @Query("""
            select (count(distinct rb) > 0)
            from RoomBooking rb
            left join rb.participants participant
            where rb.timeSlot.id = :timeSlotId
              and (
                    rb.bookingDate = :bookingDate
                    or (rb.bookingDate is null and rb.semester = :semester)
              )
              and (rb.bookedBy.id = :studentId or participant.id = :studentId)
            """)
    boolean existsForStudentAtTime(
            @Param("studentId") Long studentId,
            @Param("timeSlotId") Long timeSlotId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("semester") String semester);

    @Query("""
            select count(distinct rb)
            from RoomBooking rb
            left join rb.participants participant
            where (
                    rb.bookingDate = :bookingDate
                    or (
                        rb.bookingDate is null
                        and rb.semester = :semester
                        and rb.timeSlot.dayOfWeek = :dayOfWeek
                    )
              )
              and (rb.bookedBy.id = :studentId or participant.id = :studentId)
            """)
    long countForStudentOnDate(
            @Param("studentId") Long studentId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("semester") String semester,
            @Param("dayOfWeek") DayOfWeek dayOfWeek);
}
