package com.hospital.billing.admission.web;

import com.hospital.billing.admission.domain.AdmissionStatus;
import com.hospital.billing.admission.service.AdmissionLifecycleOperations;
import com.hospital.billing.admission.service.AdmissionReadOperations;
import com.hospital.billing.admission.service.BedAllocationOperations;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionResponse;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionSummary;
import com.hospital.billing.admission.web.AdmissionDtos.AdmissionTypeResponse;
import com.hospital.billing.admission.web.AdmissionDtos.AdmitRequest;
import com.hospital.billing.admission.web.AdmissionDtos.ChangeDoctorRequest;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationRequest;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationResponse;
import com.hospital.billing.admission.web.AdmissionDtos.DischargeRequest;
import com.hospital.billing.common.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Depends on three narrow interfaces rather than one fat "AdmissionService" (ISP + DIP). */
@Tag(name = "Admissions & Beds")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionReadOperations admissionReads;
    private final AdmissionLifecycleOperations admissionLifecycle;
    private final BedAllocationOperations bedAllocations;

    @GetMapping("/admissions")
    public PageResponse<AdmissionSummary> search(
            @RequestParam(required = false) AdmissionStatus status,
            @PageableDefault(size = 20, sort = "admissionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return admissionReads.search(status, pageable);
    }

    @GetMapping("/admissions/{id}")
    public AdmissionResponse get(@PathVariable Long id) {
        return admissionReads.get(id);
    }

    @GetMapping("/patients/{patientId}/admissions")
    public List<AdmissionSummary> byPatient(@PathVariable Long patientId) {
        return admissionReads.findByPatient(patientId);
    }

    @GetMapping("/admission-types")
    public List<AdmissionTypeResponse> admissionTypes() {
        return admissionReads.admissionTypes();
    }

    @PostMapping("/admissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public AdmissionResponse admit(@Valid @RequestBody AdmitRequest request) {
        return admissionLifecycle.admit(request);
    }

    @PostMapping("/admissions/{id}/discharge")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public AdmissionResponse discharge(@PathVariable Long id, @RequestBody(required = false) DischargeRequest request) {
        return admissionLifecycle.discharge(id, request);
    }

    @PutMapping("/admissions/{id}/doctor")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
    public AdmissionResponse changeAttendingDoctor(@PathVariable Long id,
                                                   @Valid @RequestBody ChangeDoctorRequest request) {
        return admissionLifecycle.changeAttendingDoctor(id, request);
    }

    @PostMapping("/admissions/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public AdmissionResponse cancel(@PathVariable Long id) {
        return admissionLifecycle.cancel(id);
    }

    @PostMapping("/admissions/{id}/bed")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public BedAllocationResponse allocateBed(@PathVariable Long id, @Valid @RequestBody BedAllocationRequest request) {
        return bedAllocations.allocateBed(id, request);
    }
}
