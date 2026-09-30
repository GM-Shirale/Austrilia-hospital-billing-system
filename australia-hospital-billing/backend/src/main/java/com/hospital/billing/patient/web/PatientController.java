package com.hospital.billing.patient.web;

import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.patient.service.PatientService;
import com.hospital.billing.patient.web.PatientDtos.ContactRequest;
import com.hospital.billing.patient.web.PatientDtos.PatientRequest;
import com.hospital.billing.patient.web.PatientDtos.PatientResponse;
import com.hospital.billing.patient.web.PatientDtos.PatientSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patients")
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private static final String CAN_EDIT = "hasAnyRole('ADMIN','RECEPTIONIST')";

    private final PatientService patientService;

    @Operation(summary = "Search patients by name, MRN, Medicare number or phone (paged)")
    @GetMapping
    public PageResponse<PatientSummary> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return patientService.search(q, pageable);
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable Long id) {
        return patientService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CAN_EDIT)
    public PatientResponse register(@Valid @RequestBody PatientRequest request) {
        return patientService.register(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_EDIT)
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request);
    }

    @PostMapping("/{id}/contacts")
    @PreAuthorize(CAN_EDIT)
    public PatientResponse addContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return patientService.addContact(id, request);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @PreAuthorize(CAN_EDIT)
    public PatientResponse removeContact(@PathVariable Long id, @PathVariable Long contactId) {
        return patientService.removeContact(id, contactId);
    }
}
