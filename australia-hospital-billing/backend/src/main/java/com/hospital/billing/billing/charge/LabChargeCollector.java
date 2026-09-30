package com.hospital.billing.billing.charge;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.lab.domain.LabOrderItem;
import com.hospital.billing.lab.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pathology and imaging. The ER diagram's LAB_BILL is folded into the unified hospital
 * bill as BILL_ITEM rows of type LAB, so one invoice and one claim cover everything.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class LabChargeCollector implements ChargeCollector {

    private final LabService labService;

    @Override
    public List<ChargeLine> collect(Admission admission, LocalDateTime asOf) {
        return labService.chargeableOrders(admission.getId()).stream()
                .flatMap(order -> order.getItems().stream()
                        .map(item -> toLine(order.getOrderNo(), item)))
                .toList();
    }

    private ChargeLine toLine(String orderNo, LabOrderItem item) {
        String description = "%s - %s (%s)".formatted(item.getLabTest().getCategory(),
                item.getLabTest().getTestName(), orderNo);
        return new ChargeLine(BillItemType.LAB, item.getId(), description, item.getLabTest().getMbsItemNo(),
                item.getLabTest().getMbsScheduleFee(), BigDecimal.valueOf(item.getQuantity()), item.getPrice(),
                item.getLabTest().gstRate());    // GST-free for MBS pathology / imaging
    }
}
