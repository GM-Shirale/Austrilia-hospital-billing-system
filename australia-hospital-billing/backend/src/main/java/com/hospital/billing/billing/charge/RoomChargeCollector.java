package com.hospital.billing.billing.charge;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.RoomAllocation;
import com.hospital.billing.admission.service.BedAllocationOperations;
import com.hospital.billing.billing.domain.BillItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class RoomChargeCollector implements ChargeCollector {

    private final BedAllocationOperations bedAllocations;

    @Override
    public List<ChargeLine> collect(Admission admission, LocalDateTime asOf) {
        return bedAllocations.allocationsForBilling(admission.getId()).stream()
                .map(allocation -> toLine(allocation, asOf))
                .toList();
    }

    private ChargeLine toLine(RoomAllocation allocation, LocalDateTime asOf) {
        long days = allocation.billableDays(asOf);
        String description = "Accommodation - %s (%s), %d day(s)".formatted(
                allocation.getRoom().label(), allocation.getRoom().getRoomType(), days);
        return new ChargeLine(BillItemType.ROOM, allocation.getId(), description, null, null,
                BigDecimal.valueOf(days), allocation.getChargePerDay(), null);
    }
}
