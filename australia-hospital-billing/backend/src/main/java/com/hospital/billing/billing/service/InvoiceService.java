package com.hospital.billing.billing.service;

import com.hospital.billing.admission.service.BedAllocationOperations;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.domain.PaymentRepository;
import com.hospital.billing.billing.web.BillingDtos.BillResponse;
import com.hospital.billing.billing.web.InvoiceDtos.BedStay;
import com.hospital.billing.billing.web.InvoiceDtos.DoctorInfo;
import com.hospital.billing.billing.web.InvoiceDtos.HospitalInfo;
import com.hospital.billing.billing.web.InvoiceDtos.InvoicePayment;
import com.hospital.billing.billing.web.InvoiceDtos.InvoiceResponse;
import com.hospital.billing.billing.web.InvoiceDtos.PatientInfo;
import com.hospital.billing.billing.web.InvoiceDtos.StayInfo;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.tenant.Hospital;
import com.hospital.billing.tenant.HospitalRepository;
import com.hospital.billing.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Assembles the tax invoice / discharge summary from the bill, admission, patient and hospital. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    static final String GST_STATEMENT = "Medical, hospital and pathology services and PBS medicines are GST-free "
            + "(A New Tax System (Goods and Services Tax) Act 1999, Subdiv 38-B). "
            + "GST is shown only where a taxable supply is itemised.";

    private final BillingService billingService;
    private final HospitalRepository hospitalRepository;
    private final PaymentRepository paymentRepository;
    private final BedAllocationOperations bedAllocations;
    private final Clock clock;

    public InvoiceResponse invoice(Long billId) {
        BillResponse bill = billingService.get(billId);
        HospitalBill entity = billingService.requireBill(billId);
        var admission = entity.getAdmission();
        var patient = admission.getPatient();
        var doctor = admission.getDoctor();

        Hospital hospital = hospitalRepository.findById(TenantContext.requireCurrentTenant())
                .orElseThrow(() -> new EntityNotFoundException("Hospital", TenantContext.requireCurrentTenant()));

        String address = Stream.of(patient.getAddress(), patient.getSuburb(),
                        joinNonBlank(patient.getState(), patient.getPostcode()))
                .filter(Objects::nonNull).filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));

        List<BedStay> beds = bedAllocations.allocationHistory(admission.getId()).stream()
                .map(b -> new BedStay("Room %s / Bed %s".formatted(b.roomNo(), b.bedNo()), b.roomType(),
                        b.fromDate(), b.toDate(), b.chargePerDay()))
                .toList();

        List<InvoicePayment> payments = paymentRepository.findByBillIdOrderByPaymentDateAsc(billId).stream()
                .map(p -> new InvoicePayment(p.getPaymentNo(), p.getPaymentDate(), p.getPaymentMode(), p.getPaidBy(),
                        Money.nonNegative(p.getAmount().subtract(p.getRefundedAmount())), p.getTransactionNo()))
                .toList();

        return new InvoiceResponse(
                bill.billNo(),
                LocalDateTime.now(clock),
                new HospitalInfo(hospital.getName(), hospital.getAbn(), hospital.getAddressLine(), hospital.getSuburb(),
                        hospital.getState(), hospital.getPostcode(), hospital.getPhone(), hospital.getEmail()),
                new PatientInfo(patient.fullName(), patient.getMrn(), patient.getDob(), patient.getGender(), address,
                        patient.getPhone(), patient.getEmail(), patient.getMedicareNo(), patient.getMedicareIrn()),
                new DoctorInfo(doctor.displayName(), doctor.getProviderNo(), doctor.getSpecialization(),
                        doctor.getDepartment().getDepartmentName()),
                new StayInfo(admission.getAdmissionNo(), admission.getAdmissionType().getTypeName(),
                        admission.getCareSetting(), admission.getFinancialClass(), admission.getAdmissionDate(),
                        admission.getDischargeDate(), admission.lengthOfStayDays(LocalDateTime.now(clock)),
                        admission.getDiagnosis(), beds),
                bill.coverage(),
                bill,
                payments,
                GST_STATEMENT);
    }

    private static String joinNonBlank(String a, String b) {
        return Stream.of(a, b).filter(Objects::nonNull).filter(s -> !s.isBlank()).collect(Collectors.joining(" "));
    }

}
