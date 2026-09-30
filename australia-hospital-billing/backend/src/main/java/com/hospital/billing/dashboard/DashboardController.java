package com.hospital.billing.dashboard;

import com.hospital.billing.admission.domain.AdmissionRepository;
import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.domain.RoomRepository;
import com.hospital.billing.admission.domain.RoomStatus;
import com.hospital.billing.billing.domain.BillStatus;
import com.hospital.billing.billing.domain.HospitalBillRepository;
import com.hospital.billing.billing.domain.PaymentRepository;
import com.hospital.billing.claim.domain.InsuranceClaimRepository;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.lab.domain.LabOrderRepository;
import com.hospital.billing.lab.domain.LabOrderStatus;
import com.hospital.billing.patient.domain.PatientRepository;
import com.hospital.billing.patient.domain.PatientStatus;
import com.hospital.billing.pharmacy.domain.MedicineRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only KPIs for the signed-in hospital (all queries are tenant-filtered by Hibernate). */
@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final RoomRepository roomRepository;
    private final LabOrderRepository labOrderRepository;
    private final MedicineRepository medicineRepository;
    private final HospitalBillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final InsuranceClaimRepository claimRepository;

    public record DashboardSummary(long activePatients, long currentAdmissions, long availableBeds, long occupiedBeds,
                                   long pendingLabOrders, long lowStockMedicines, long draftBills,
                                   BigDecimal outstandingAmount, BigDecimal collectedAmount,
                                   Map<String, Long> claimsByStatus) {
    }

    @Operation(summary = "Headline numbers for the dashboard")
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        Map<String, Long> claims = new LinkedHashMap<>();
        for (Object[] row : claimRepository.countGroupedByStatus()) {
            claims.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return new DashboardSummary(
                patientRepository.countByStatus(PatientStatus.ACTIVE),
                admissionRepository.countByStatus(AdmissionStatus.ADMITTED),
                roomRepository.countByStatus(RoomStatus.AVAILABLE),
                roomRepository.countByStatus(RoomStatus.OCCUPIED),
                labOrderRepository.countByStatusIn(List.of(LabOrderStatus.ORDERED, LabOrderStatus.COLLECTED)),
                medicineRepository.countByStockQuantityLessThan(100),
                billRepository.countByStatus(BillStatus.DRAFT),
                Money.of(billRepository.sumOutstanding(EnumSet.of(BillStatus.FINALIZED, BillStatus.PARTIALLY_PAID))),
                Money.of(paymentRepository.sumCollected()),
                claims);
    }
}
