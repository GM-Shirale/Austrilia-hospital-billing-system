package com.hospital.billing.billing.charge;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.pharmacy.domain.PrescriptionItem;
import com.hospital.billing.pharmacy.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Only DISPENSED medicines are charged; the medicine's own GST rate is carried through. */
@Component
@Order(4)
@RequiredArgsConstructor
public class PharmacyChargeCollector implements ChargeCollector {

    private final PharmacyService pharmacyService;

    @Override
    public List<ChargeLine> collect(Admission admission, LocalDateTime asOf) {
        return pharmacyService.dispensedPrescriptions(admission.getId()).stream()
                .flatMap(rx -> rx.getItems().stream().map(item -> toLine(rx.getPrescriptionNo(), item)))
                .toList();
    }

    private ChargeLine toLine(String prescriptionNo, PrescriptionItem item) {
        String description = "Pharmacy - %s x %d, %s %s (%s)".formatted(item.getMedicine().getMedicineName(),
                item.getQuantity(), item.getDose(), item.getFrequency(), prescriptionNo);
        return new ChargeLine(BillItemType.PHARMACY, item.getId(), description, null, null,
                BigDecimal.valueOf(item.getQuantity()), item.getUnitPrice(), item.getMedicine().getGstRate());
    }
}
