package com.hospital.billing.pharmacy.web;

import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.pharmacy.domain.PrescriptionStatus;
import com.hospital.billing.pharmacy.service.PharmacyService;
import com.hospital.billing.pharmacy.web.PharmacyDtos.MedicineRequest;
import com.hospital.billing.pharmacy.web.PharmacyDtos.MedicineResponse;
import com.hospital.billing.pharmacy.web.PharmacyDtos.PrescriptionRequest;
import com.hospital.billing.pharmacy.web.PharmacyDtos.PrescriptionResponse;
import com.hospital.billing.pharmacy.web.PharmacyDtos.StockAdjustmentRequest;
import com.hospital.billing.pharmacy.web.PharmacyDtos.StockMovementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pharmacy")
@RestController
@RequestMapping("/api/v1/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @Operation(summary = "Medicine catalogue; activeOnly=true for prescription forms")
    @GetMapping("/medicines")
    public List<MedicineResponse> medicines(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return pharmacyService.medicines(activeOnly);
    }

    @PutMapping("/medicines/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACY')")
    public MedicineResponse updateMedicine(@PathVariable Long id, @Valid @RequestBody MedicineRequest request) {
        return pharmacyService.updateMedicine(id, request);
    }

    @Operation(summary = "Stock ledger of a medicine (latest 50 movements, with who and why)")
    @GetMapping("/medicines/{id}/movements")
    public List<StockMovementResponse> stockMovements(@PathVariable Long id) {
        return pharmacyService.stockMovements(id);
    }

    @Operation(summary = "Pharmacy work queue: prescriptions awaiting dispensing, oldest first")
    @GetMapping("/queue")
    public List<PrescriptionResponse> queue() {
        return pharmacyService.queue();
    }

    @PostMapping("/medicines")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACY')")
    public MedicineResponse createMedicine(@Valid @RequestBody MedicineRequest request) {
        return pharmacyService.createMedicine(request);
    }

    @PatchMapping("/medicines/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACY')")
    public MedicineResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return pharmacyService.adjustStock(id, request.delta(), request.reason());
    }

    @GetMapping("/prescriptions")
    public PageResponse<PrescriptionResponse> prescriptions(
            @RequestParam(required = false) PrescriptionStatus status,
            @PageableDefault(size = 20, sort = "prescriptionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return pharmacyService.prescriptions(status, pageable);
    }

    @GetMapping("/prescriptions/by-admission/{admissionId}")
    public List<PrescriptionResponse> prescriptionsForAdmission(@PathVariable Long admissionId) {
        return pharmacyService.prescriptionsForAdmission(admissionId);
    }

    @GetMapping("/prescriptions/{id}")
    public PrescriptionResponse prescription(@PathVariable Long id) {
        return pharmacyService.prescription(id);
    }

    @PostMapping("/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public PrescriptionResponse prescribe(@Valid @RequestBody PrescriptionRequest request) {
        return pharmacyService.prescribe(request);
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACY','DOCTOR')")
    public PrescriptionResponse dispense(@PathVariable Long id) {
        return pharmacyService.dispense(id);
    }

    @PostMapping("/prescriptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','PHARMACY')")
    public PrescriptionResponse cancel(@PathVariable Long id) {
        return pharmacyService.cancel(id);
    }
}
