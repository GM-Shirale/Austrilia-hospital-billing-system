package com.hospital.billing.admission.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomAllocationTest {

    private static final LocalDateTime ADMITTED = LocalDateTime.of(2026, 9, 1, 10, 0);

    @Test
    void partDayIsChargedAsAFullDayWithMinimumOne() {
        RoomAllocation allocation = allocation();
        assertEquals(1, allocation.billableDays(ADMITTED.plusHours(3)));
        assertEquals(1, allocation.billableDays(ADMITTED.plusHours(24)));
        assertEquals(2, allocation.billableDays(ADMITTED.plusHours(25)));
    }

    @Test
    void closedStayUsesItsEndDate() {
        RoomAllocation allocation = allocation();
        allocation.close(ADMITTED.plusDays(3));
        assertEquals(3, allocation.billableDays(ADMITTED.plusDays(30)));
    }

    @Test
    void occupiedBedCannotBeAllocatedAgain() {
        Room room = new Room();
        room.setRoomNo("101");
        room.setBedNo("A");
        room.occupy();
        assertThrows(RuntimeException.class, room::occupy);
        room.release();
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    private static RoomAllocation allocation() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setFromDate(ADMITTED);
        return allocation;
    }
}
