package com.hospital.billing.clinical;

import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.InvalidStateTransitionException;
import com.hospital.billing.lab.domain.LabOrder;
import com.hospital.billing.lab.domain.LabOrderStatus;
import com.hospital.billing.lab.domain.LabTest;
import com.hospital.billing.pharmacy.domain.Medicine;
import com.hospital.billing.pharmacy.domain.Prescription;
import com.hospital.billing.pharmacy.domain.PrescriptionStatus;
import com.hospital.billing.pharmacy.domain.SupplyCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClinicalAuditAndGstTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 28, 9, 30);

    @Test
    @DisplayName("GST follows the supply category: PBS and prescription GST-free, OTC and taxable 10%")
    void medicineGstFollowsCategory() {
        Medicine medicine = new Medicine();
        medicine.classify(SupplyCategory.PBS);
        assertEquals(0, medicine.getGstRate().signum());
        medicine.classify(SupplyCategory.PRESCRIPTION);
        assertEquals(0, medicine.getGstRate().signum());
        medicine.classify(SupplyCategory.OTC);
        assertEquals(new BigDecimal("0.1000"), medicine.getGstRate());
        medicine.classify(SupplyCategory.TAXABLE_SUPPLY);
        assertEquals(new BigDecimal("0.1000"), medicine.getGstRate());
    }

    @Test
    @DisplayName("MBS pathology / imaging tests are GST-free; a taxable test carries 10%")
    void labTestGst() {
        LabTest test = new LabTest();
        assertEquals(0, test.gstRate().signum());
        test.setGstFree(false);
        assertEquals(new BigDecimal("0.10"), test.gstRate());
    }

    @Test
    @DisplayName("Stock changes record who adjusted it and never go below zero")
    void stockChangesAreStampedAndGuarded() {
        Medicine medicine = new Medicine();
        medicine.setMedicineName("Paracetamol 500mg tablet");
        medicine.setStockQuantity(10);

        assertEquals(4, medicine.changeStock(-6, "Pat Pharmacist (pharmacy)", AT));
        assertEquals("Pat Pharmacist (pharmacy)", medicine.getStockAdjustedByName());
        assertEquals(AT, medicine.getStockAdjustedAt());
        assertThrows(BusinessRuleException.class, () -> medicine.changeStock(-5, "x", AT));
        assertEquals(Integer.valueOf(4), medicine.getStockQuantity());
    }

    @Test
    @DisplayName("Lab order records who collected and who performed it, with result notes")
    void labOrderAuditTrail() {
        LabOrder order = new LabOrder();
        order.transition(LabOrderStatus.COLLECTED, 5L, "Lee Tech (lab)", AT, null);
        order.transition(LabOrderStatus.COMPLETED, 6L, "Sam Scientist (lab)", AT.plusHours(2), "Hb 132 g/L, normal");

        assertEquals(LabOrderStatus.COMPLETED, order.getStatus());
        assertEquals("Lee Tech (lab)", order.getCollectedByName());
        assertEquals("Sam Scientist (lab)", order.getPerformedByName());
        assertEquals(Long.valueOf(6L), order.getPerformedByUserId());
        assertEquals(AT.plusHours(2), order.getCompletedAt());
        assertEquals("Hb 132 g/L, normal", order.getResultNotes());
        assertThrows(InvalidStateTransitionException.class,
                () -> order.transition(LabOrderStatus.CANCELLED, 1L, "x", AT, null));
    }

    @Test
    @DisplayName("Prescription records the dispensing pharmacist and can only be dispensed once")
    void prescriptionAuditTrail() {
        Prescription rx = new Prescription();
        rx.setPrescribedByName("Dr Priya Sharma (doctor)");
        rx.markDispensed(9L, "Pat Pharmacist (pharmacy)", AT);

        assertEquals(PrescriptionStatus.DISPENSED, rx.getStatus());
        assertEquals("Pat Pharmacist (pharmacy)", rx.getDispensedByName());
        assertEquals(AT, rx.getDispensedAt());
        assertThrows(InvalidStateTransitionException.class, () -> rx.markDispensed(9L, "again", AT));
        assertThrows(InvalidStateTransitionException.class, () -> rx.cancel("x", AT));
    }
}
