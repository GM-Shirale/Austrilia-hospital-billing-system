package com.hospital.billing.billing.charge;

import com.hospital.billing.billing.domain.BillItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GstCalculatorTest {

    private final GstCalculator calculator = new GstCalculator(new BigDecimal("0.10"));

    @Test
    void medicalServicesAndAccommodationAreGstFree() {
        assertEquals(BigDecimal.ZERO, calculator.rateFor(BillItemType.ROOM, null));
        assertEquals(BigDecimal.ZERO, calculator.rateFor(BillItemType.DOCTOR, null));
    }

    @Test
    void equipmentUsesStandardRateAndMedicineRateWins() {
        assertEquals(new BigDecimal("0.10"), calculator.rateFor(BillItemType.EQUIPMENT, null));
        assertEquals(new BigDecimal("0.1000"), calculator.rateFor(BillItemType.PHARMACY, new BigDecimal("0.1000")));
        assertEquals(new BigDecimal("3.80"), calculator.gst(new BigDecimal("38.00"), new BigDecimal("0.10")));
    }
}
