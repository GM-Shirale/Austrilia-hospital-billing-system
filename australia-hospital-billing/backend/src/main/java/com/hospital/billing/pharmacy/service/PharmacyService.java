package com.hospital.billing.pharmacy.service;

import com.hospital.billing.common.event.ChargeSourceChangedEvent;
import com.hospital.billing.pharmacy.domain.StockMovement;
import com.hospital.billing.pharmacy.domain.StockMovementRepository;
import com.hospital.billing.pharmacy.domain.StockMovementType;
import com.hospital.billing.pharmacy.domain.SupplyCategory;
import com.hospital.billing.pharmacy.web.PharmacyDtos.StockMovementResponse;
import com.hospital.billing.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.service.AdmissionReadOperations;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.pharmacy.domain.Medicine;
import com.hospital.billing.pharmacy.domain.MedicineRepository;
import com.hospital.billing.pharmacy.domain.Prescription;
import com.hospital.billing.pharmacy.domain.PrescriptionItem;
import com.hospital.billing.pharmacy.domain.PrescriptionRepository;
import com.hospital.billing.pharmacy.domain.PrescriptionStatus;
import com.hospital.billing.pharmacy.web.PharmacyDtos.MedicineRequest;
import com.hospital.billing.pharmacy.web.PharmacyDtos.MedicineResponse;
import com.hospital.billing.pharmacy.web.PharmacyDtos.PrescriptionRequest;
import com.hospital.billing.pharmacy.web.PharmacyDtos.PrescriptionResponse;
import com.hospital.billing.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacyService {

    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AdmissionReadOperations admissionReads;
    private final ProviderService providerService;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final StockMovementRepository stockMovementRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public List<MedicineResponse> medicines(boolean activeOnly) {
        List<Medicine> list = activeOnly
                ? medicineRepository.findByActiveTrueOrderByMedicineNameAsc()
                : medicineRepository.findAllByOrderByMedicineNameAsc();
        return list.stream().map(MedicineResponse::from).toList();
    }

    @Transactional
    public MedicineResponse createMedicine(MedicineRequest request) {
        if (medicineRepository.existsByMedicineNameIgnoreCase(request.medicineName().trim())) {
            throw new DuplicateResourceException("Medicine '%s' already exists".formatted(request.medicineName()));
        }
        Medicine medicine = new Medicine();
        medicine.setMedicineName(request.medicineName().trim());
        apply(medicine, request);
        medicine.setStockQuantity(0);
        Medicine saved = medicineRepository.save(medicine);
        int opening = request.stockQuantity() == null ? 0 : request.stockQuantity();
        if (opening > 0) {
            moveStock(saved, StockMovementType.OPENING, opening, "Opening stock", null);
        }
        return MedicineResponse.from(saved);
    }

    /** Name and stock are not edited here (stock only moves through the ledger). */
    @Transactional
    public MedicineResponse updateMedicine(Long id, MedicineRequest request) {
        Medicine medicine = requireMedicine(id);
        apply(medicine, request);
        return MedicineResponse.from(medicine);
    }

    private static void apply(Medicine medicine, MedicineRequest request) {
        medicine.setCategory(request.category().trim());
        medicine.setUnitPrice(Money.of(request.unitPrice()));
        medicine.classify(request.supplyCategory());          // GST follows the category
        medicine.setPbsItemCode(request.supplyCategory() == SupplyCategory.PBS
                && request.pbsItemCode() != null && !request.pbsItemCode().isBlank()
                ? request.pbsItemCode().trim() : null);
        medicine.setActive(request.active() == null || request.active());
    }

    @Transactional
    public MedicineResponse adjustStock(Long medicineId, int delta, String reason) {
        if (delta == 0) {
            throw new BusinessRuleException("Enter a non-zero quantity");
        }
        Medicine medicine = requireMedicine(medicineId);
        String why = reason == null || reason.isBlank() ? (delta > 0 ? "Stock received" : "Stock write-off") : reason.trim();
        moveStock(medicine, delta > 0 ? StockMovementType.RECEIPT : StockMovementType.ADJUSTMENT, delta, why, null);
        return MedicineResponse.from(medicine);
    }

    public List<StockMovementResponse> stockMovements(Long medicineId) {
        requireMedicine(medicineId);
        return stockMovementRepository.findTop50ByMedicineIdOrderByPerformedAtDescIdDesc(medicineId).stream()
                .map(StockMovementResponse::from)
                .toList();
    }

    /** Changes stock and writes the ledger row, stamped with the signed-in user. */
    private void moveStock(Medicine medicine, StockMovementType type, int delta, String reason, Prescription rx) {
        LocalDateTime now = LocalDateTime.now(clock);
        int balance = medicine.changeStock(delta, CurrentUser.displayName(), now);
        StockMovement movement = new StockMovement();
        movement.setMedicine(medicine);
        movement.setMovementType(type);
        movement.setQuantityDelta(delta);
        movement.setBalanceAfter(balance);
        movement.setReason(reason);
        movement.setPrescription(rx);
        movement.setPerformedByUserId(CurrentUser.userIdOrNull());
        movement.setPerformedByName(CurrentUser.displayName());
        movement.setPerformedAt(now);
        stockMovementRepository.save(movement);
    }

    private Medicine requireMedicine(Long id) {
        return medicineRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Medicine", id));
    }

    /** Pharmacy queue: prescriptions awaiting dispensing, oldest first. */
    public List<PrescriptionResponse> queue() {
        return prescriptionRepository.findByStatusOrderByPrescriptionDateAsc(PrescriptionStatus.PRESCRIBED).stream()
                .map(PrescriptionResponse::detail)
                .toList();
    }

    /** For the bill: prescriptions that are not dispensed yet (so not billed yet). */
    public long countAwaitingDispense(Long admissionId) {
        return prescriptionRepository.countByAdmissionIdAndStatus(admissionId, PrescriptionStatus.PRESCRIBED);
    }

    public PageResponse<PrescriptionResponse> prescriptions(PrescriptionStatus status, Pageable pageable) {
        Page<Prescription> page = status == null
                ? prescriptionRepository.findAll(pageable)
                : prescriptionRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(PrescriptionResponse::summary));
    }

    public PrescriptionResponse prescription(Long id) {
        return PrescriptionResponse.detail(requirePrescription(id));
    }

    @Transactional
    public PrescriptionResponse prescribe(PrescriptionRequest request) {
        Admission admission = admissionReads.requireAdmission(request.admissionId());
        admission.requireActive();

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(documentNumberGenerator.next(DocumentType.PRESCRIPTION));
        prescription.setAdmission(admission);
        prescription.setDoctor(providerService.requireActiveDoctor(request.doctorId()));
        prescription.setPrescriptionDate(LocalDateTime.now(clock));
        prescription.setPrescribedByUserId(CurrentUser.userIdOrNull());
        prescription.setPrescribedByName(CurrentUser.displayName());
        request.items().forEach(line -> {
            Medicine medicine = medicineRepository.findById(line.medicineId())
                    .orElseThrow(() -> new EntityNotFoundException("Medicine", line.medicineId()));
            if (!medicine.isActive()) {
                throw new BusinessRuleException("%s is no longer stocked".formatted(medicine.getMedicineName()));
            }
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicine(medicine);
            item.setQuantity(line.quantity());
            item.setDose(line.dose().trim());
            item.setFrequency(line.frequency().trim());
            item.setUnitPrice(medicine.getUnitPrice());
            prescription.addItem(item);
        });
        return PrescriptionResponse.detail(prescriptionRepository.save(prescription));
    }

    /** Optimistic locking on Medicine (@Version) protects stock from concurrent dispensing. */
    @Transactional
    public PrescriptionResponse dispense(Long id) {
        Prescription prescription = requirePrescription(id);
        prescription.getAdmission().requireActive();    // nothing can be added to a locked (discharged) bill
        prescription.markDispensed(CurrentUser.userIdOrNull(), CurrentUser.displayName(), LocalDateTime.now(clock));
        prescription.getItems().forEach(item -> moveStock(item.getMedicine(), StockMovementType.DISPENSE,
                -item.getQuantity(), "Dispensed for " + prescription.getAdmission().getAdmissionNo(), prescription));
        // Dispensed medicines are billable: bring the admission's draft bill up to date.
        events.publishEvent(new ChargeSourceChangedEvent(prescription.getAdmission().getId(),
                "Prescription " + prescription.getPrescriptionNo() + " dispensed"));
        return PrescriptionResponse.detail(prescription);
    }

    @Transactional
    public PrescriptionResponse cancel(Long id) {
        Prescription prescription = requirePrescription(id);
        prescription.getAdmission().requireActive();
        prescription.cancel(CurrentUser.displayName(), LocalDateTime.now(clock));
        return PrescriptionResponse.detail(prescription);
    }

    public List<PrescriptionResponse> prescriptionsForAdmission(Long admissionId) {
        return prescriptionRepository.findByAdmissionIdOrderByPrescriptionDateDesc(admissionId).stream()
                .map(PrescriptionResponse::detail)
                .toList();
    }

    /** For billing: only dispensed medicines are charged. */
    public List<Prescription> dispensedPrescriptions(Long admissionId) {
        return prescriptionRepository.findByAdmissionIdAndStatus(admissionId, PrescriptionStatus.DISPENSED);
    }

    private Prescription requirePrescription(Long id) {
        return prescriptionRepository.findWithItemsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prescription", id));
    }
}
