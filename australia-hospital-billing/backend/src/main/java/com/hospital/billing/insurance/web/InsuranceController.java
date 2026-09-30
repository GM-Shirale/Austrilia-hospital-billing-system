package com.hospital.billing.insurance.web;

import com.hospital.billing.insurance.service.InsuranceService;
import com.hospital.billing.insurance.web.InsuranceDtos.CompanyRequest;
import com.hospital.billing.insurance.web.InsuranceDtos.CompanyResponse;
import com.hospital.billing.insurance.web.InsuranceDtos.PolicyRequest;
import com.hospital.billing.insurance.web.InsuranceDtos.PolicyResponse;
import com.hospital.billing.insurance.web.InsuranceDtos.PolicyStatusRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Insurance (payers & policies)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @GetMapping("/insurance/companies")
    public List<CompanyResponse> companies() {
        return insuranceService.companies();
    }

    @PostMapping("/insurance/companies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OFFICER')")
    public CompanyResponse createCompany(@Valid @RequestBody CompanyRequest request) {
        return insuranceService.createCompany(request);
    }

    @GetMapping("/patients/{patientId}/policies")
    public List<PolicyResponse> policies(@PathVariable Long patientId) {
        return insuranceService.policiesForPatient(patientId);
    }

    @PostMapping("/patients/{patientId}/policies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OFFICER','RECEPTIONIST')")
    public PolicyResponse createPolicy(@PathVariable Long patientId, @Valid @RequestBody PolicyRequest request) {
        return insuranceService.createPolicy(patientId, request);
    }

    @PatchMapping("/insurance/policies/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OFFICER')")
    public PolicyResponse changeStatus(@PathVariable Long id, @Valid @RequestBody PolicyStatusRequest request) {
        return insuranceService.changeStatus(id, request.status());
    }
}
