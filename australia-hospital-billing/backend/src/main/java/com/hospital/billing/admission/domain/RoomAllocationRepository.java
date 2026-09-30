package com.hospital.billing.admission.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long> {

    @EntityGraph(attributePaths = "room")
    List<RoomAllocation> findByAdmissionIdOrderByFromDateAsc(Long admissionId);

    @EntityGraph(attributePaths = "room")
    Optional<RoomAllocation> findFirstByAdmissionIdAndToDateIsNull(Long admissionId);

    /**
     * Temporal overlap check, served by ix_allocation_room_period (room_id, from_date, to_date).
     * Two periods [a1,a2) and [b1,b2) overlap when a1 < b2 AND b1 < a2; an open period
     * (to_date IS NULL) extends to infinity.
     */
    @Query("""
            select count(a) from RoomAllocation a
            where a.room.id = :roomId
              and a.fromDate < :periodEnd
              and (a.toDate is null or a.toDate > :periodStart)
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                              @Param("periodStart") LocalDateTime periodStart,
                              @Param("periodEnd") LocalDateTime periodEnd);

    boolean existsByRoomId(Long roomId);
}
