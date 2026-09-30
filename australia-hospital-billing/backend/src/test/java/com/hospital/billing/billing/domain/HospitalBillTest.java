package com.hospital.billing.billing.domain;

import com.hospital.billing.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HospitalBillTest {

    @Test
    void totalsAreDerivedFromItems() {
        HospitalBill bill = billWith(item("1000.00", "0.00", "700.00", "300.00"), item("200.00", "150.00", "0.00", "50.00"));

        assertEquals(new BigDecimal("1200.00"), bill.getNetAmount());
        assertEquals(new BigDecimal("150.00"), bill.getMedicareAmount());
        assertEquals(new BigDecimal("700.00"), bill.getInsuranceAmount());
        assertEquals(new BigDecimal("350.00"), bill.getPatientPayableAmount());
    }

    @Test
    void paymentsMoveStatusAndCannotExceedOutstanding() {
        HospitalBill bill = billWith(item("500.00", "0.00", "0.00", "500.00"));
        bill.finalizeBill();

        assertEquals(BillPaymentStatus.PENDING, bill.getPaymentStatus());

        bill.registerPayment(new BigDecimal("200.00"), PaidBy.PATIENT);
        assertEquals(BillStatus.PARTIALLY_PAID, bill.getStatus());
        assertEquals(BillPaymentStatus.PARTIAL, bill.getPaymentStatus());

        assertThrows(BusinessRuleException.class, () -> bill.registerPayment(new BigDecimal("300.01"), PaidBy.PATIENT));

        bill.registerPayment(new BigDecimal("300.00"), PaidBy.PATIENT);
        assertEquals(BillStatus.PAID, bill.getStatus());
        assertEquals(BillPaymentStatus.FULL, bill.getPaymentStatus());
        assertEquals(new BigDecimal("0.00"), bill.outstandingAmount());
        assertEquals(new BigDecimal("0.00"), bill.patientBalance());

        bill.reversePayment(new BigDecimal("100.00"), PaidBy.PATIENT);
        assertEquals(BillStatus.PARTIALLY_PAID, bill.getStatus());
        assertEquals(BillPaymentStatus.PARTIAL, bill.getPaymentStatus());
        assertEquals(new BigDecimal("100.00"), bill.patientBalance());
    }

    @Test
    void remittancesDoNotSettleThePatientShare() {
        HospitalBill bill = billWith(item("1000.00", "600.00", "0.00", "400.00"));
        bill.finalizeBill();

        bill.registerPayment(new BigDecimal("600.00"), PaidBy.MEDICARE);
        assertEquals(BillStatus.PARTIALLY_PAID, bill.getStatus());
        assertEquals(BillPaymentStatus.PENDING, bill.getPaymentStatus());
        assertEquals(new BigDecimal("400.00"), bill.patientBalance());
    }

    @Test
    void fullyFundedPublicPatientIsFullOnceFinalised() {
        HospitalBill bill = billWith(item("800.00", "800.00", "0.00", "0.00"));
        assertEquals(BillPaymentStatus.PENDING, bill.getPaymentStatus());
        bill.finalizeBill();
        assertEquals(BillPaymentStatus.FULL, bill.getPaymentStatus());
    }

    @Test
    void dischargeLocksClinicalLinesButNotManualOnes() {
        BillItem clinical = item("450.00", "0.00", "0.00", "450.00");
        clinical.setReferenceId(7L);
        BillItem manual = item("20.00", "0.00", "0.00", "20.00");
        HospitalBill bill = billWith(clinical, manual);

        bill.lockCharges(java.time.LocalDateTime.of(2026, 9, 20, 10, 0));

        assertEquals(true, bill.isChargesLocked());
        assertThrows(BusinessRuleException.class, () -> bill.removeItem(clinical));
        bill.removeItem(manual);
        assertEquals(1, bill.getItems().size());
    }

    @Test
    void finalisedBillCannotBeEdited() {
        HospitalBill bill = billWith(item("100.00", "0.00", "0.00", "100.00"));
        bill.finalizeBill();

        assertThrows(BusinessRuleException.class, () -> bill.addItem(item("1.00", "0.00", "0.00", "1.00")));
    }

    @Test
    void rejectedBenefitShiftsToPatient() {
        BillItem line = item("300.00", "0.00", "250.00", "50.00");
        HospitalBill bill = billWith(line);

        line.shiftToPatient(PaidBy.PRIVATE_FUND, new BigDecimal("250.00"));
        bill.recalculateTotals();

        assertEquals(new BigDecimal("0.00"), bill.getInsuranceAmount());
        assertEquals(new BigDecimal("300.00"), bill.getPatientPayableAmount());
    }

    private static HospitalBill billWith(BillItem... items) {
        HospitalBill bill = new HospitalBill();
        bill.setBillNo("BILL-TEST");
        for (BillItem item : items) {
            bill.addItem(item);
        }
        bill.recalculateTotals();
        return bill;
    }

    private static BillItem item(String net, String medicare, String fund, String patient) {
        BillItem item = new BillItem();
        item.setItemType(BillItemType.OTHER);
        item.setDescription("test line");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal(net));
        item.setGrossAmount(new BigDecimal(net));
        item.setNetAmount(new BigDecimal(net));
        item.applySplit(new BigDecimal(medicare), new BigDecimal(fund), new BigDecimal(patient));
        return item;
    }
}
