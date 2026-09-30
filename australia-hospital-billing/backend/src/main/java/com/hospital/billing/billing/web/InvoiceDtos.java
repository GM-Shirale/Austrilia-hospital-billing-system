package com.hospital.billing.billing.web;

import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.admission.domain.RoomType;
import com.hospital.billing.billing.domain.PaidBy;
import com.hospital.billing.billing.domain.PaymentMode;
import com.hospital.billing.billing.web.BillingDtos.BillResponse;
import com.hospital.billing.billing.web.BillingDtos.CoverageSummary;
import com.hospital.billing.patient.domain.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Everything printed on the "Tax Invoice / Discharge Summary" PDF, in one call. */
public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record HospitalInfo(String name, String abn, String addressLine, String suburb, String state,
                               String postcode, String phone, String email) {
    }

    public record PatientInfo(String name, String mrn, LocalDate dob, Gender gender, String address, String phone,
                              String email, String medicareNo, Short medicareIrn) {
    }

    public record DoctorInfo(String name, String providerNo, String specialization, String department) {
    }

    public record BedStay(String room, RoomType roomType, LocalDateTime fromDate, LocalDateTime toDate,
                          BigDecimal ratePerDay) {
    }

    public record StayInfo(String admissionNo, String admissionType, CareSetting careSetting,
                           FinancialClass financialClass, LocalDateTime admissionDate, LocalDateTime dischargeDate,
                           long lengthOfStayDays, String diagnosis, List<BedStay> beds) {
    }

    public record InvoicePayment(String paymentNo, LocalDateTime paymentDate, PaymentMode paymentMode, PaidBy paidBy,
                                 BigDecimal amount, String transactionNo) {
    }

    public record InvoiceResponse(String invoiceNo, LocalDateTime issuedAt, HospitalInfo hospital,
                                  PatientInfo patient, DoctorInfo attendingDoctor, StayInfo stay,
                                  CoverageSummary coverage, BillResponse bill, List<InvoicePayment> payments,
                                  String gstStatement) {
    }
}
