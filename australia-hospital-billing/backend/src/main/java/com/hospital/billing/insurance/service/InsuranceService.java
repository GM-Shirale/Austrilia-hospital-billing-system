package com.hospital.billing.insurance.service;

import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.InsuranceCompany;
import com.hospital.billing.insurance.domain.InsuranceCompanyRepository;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.InsurancePolicyRepository;
import com.hospital.billing.insurance.domain.NetworkStatus;
import com.hospital.billing.insurance.domain.PayerType;
import com.hospital.billing.insurance.domain.PolicyStatus;
import com.hospital.billing.insurance.web.InsuranceDtos.CompanyRequest;
import com.hospital.billing.insurance.web.InsuranceDtos.CompanyResponse;
import com.hospital.billing.insurance.web.InsuranceDtos.PolicyRequest;
import com.hospital.billing.insurance.web.InsuranceDtos.PolicyResponse;
import com.hospital.billing.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceService {

    private final InsuranceCompanyRepository companyRepository;
    private final InsurancePolicyRepository policyRepository;
    private final PatientService patientService;

    public List<CompanyResponse> companies() {
        return companyRepository.findAllByOrderByCompanyNameAsc().stream().map(CompanyResponse::from).toList();
    }

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        if (companyRepository.existsByCompanyNameIgnoreCase(request.companyName().trim())) {
            throw new DuplicateResourceException("Payer '%s' already exists".formatted(request.companyName()));
        }
        if (request.payerType() == PayerType.MEDICARE && companyRepository.findFirstByPayerType(PayerType.MEDICARE).isPresent()) {
            throw new DuplicateResourceException("Medicare is already configured for this hospital");
        }
        if (request.networkStatus() == NetworkStatus.KNOWN_GAP && request.knownGapCap() == null) {
            throw new BusinessRuleException("A known-gap agreement needs a knownGapCap");
        }
        InsuranceCompany company = new InsuranceCompany();
        company.setCompanyName(request.companyName().trim());
        company.setPayerType(request.payerType());
        company.setNetworkStatus(request.payerType() == PayerType.MEDICARE
                ? NetworkStatus.NOT_APPLICABLE : request.networkStatus());
        company.setKnownGapCap(request.knownGapCap() == null ? null : Money.of(request.knownGapCap()));
        company.setAbn(request.abn());
        company.setContactPerson(request.contactPerson());
        company.setPhone(request.phone());
        company.setEmail(request.email());
        company.setAddress(request.address());
        return CompanyResponse.from(companyRepository.save(company));
    }

    public List<PolicyResponse> policiesForPatient(Long patientId) {
        patientService.requirePatient(patientId);
        return policyRepository.findByPatientIdOrderByStartDateDesc(patientId).stream()
                .map(PolicyResponse::from)
                .toList();
    }

    @Transactional
    public PolicyResponse createPolicy(Long patientId, PolicyRequest request) {
        InsuranceCompany company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("Insurance company", request.companyId()));
        if (company.getPayerType() != PayerType.PRIVATE_FUND) {
            throw new BusinessRuleException("Policies can only be issued by private funds (Medicare uses the Medicare card)");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("Policy end date must be after the start date");
        }
        if (policyRepository.existsByCompanyIdAndPolicyNoIgnoreCase(company.getId(), request.policyNo().trim())) {
            throw new DuplicateResourceException("Policy %s already exists for %s"
                    .formatted(request.policyNo(), company.getCompanyName()));
        }
        InsurancePolicy policy = new InsurancePolicy();
        policy.setPatient(patientService.requirePatient(patientId));
        policy.setCompany(company);
        policy.setPolicyNo(request.policyNo().trim().toUpperCase());
        policy.setCoverTier(request.coverTier());
        policy.setExcessAmount(Money.of(request.excessAmount()));
        policy.setAnnualLimit(Money.of(request.annualLimit()));
        policy.setStartDate(request.startDate());
        policy.setEndDate(request.endDate());
        policy.setStatus(PolicyStatus.ACTIVE);
        return PolicyResponse.from(policyRepository.save(policy));
    }

    @Transactional
    public PolicyResponse changeStatus(Long policyId, PolicyStatus status) {
        InsurancePolicy policy = requirePolicy(policyId);
        policy.setStatus(status);
        return PolicyResponse.from(policy);
    }

    // ---- used by billing / claims ------------------------------------------

    /** The patient's private cover that is valid on the given date, if any. */
    public Optional<InsurancePolicy> findActivePrivatePolicy(Long patientId, LocalDate onDate) {
        return policyRepository.findByPatientIdOrderByStartDateDesc(patientId).stream()
                .filter(p -> p.getCompany().getPayerType() == PayerType.PRIVATE_FUND)
                .filter(p -> p.isActiveOn(onDate))
                .findFirst();
    }

    public Optional<InsuranceCompany> findMedicare() {
        return companyRepository.findFirstByPayerType(PayerType.MEDICARE);
    }

    public InsurancePolicy requirePolicy(Long policyId) {
        return policyRepository.findWithCompanyById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Insurance policy", policyId));
    }

    /** Benefit accumulator update when a private fund actually pays. */
    @Transactional
    public void recordBenefitPaid(Long policyId, BigDecimal amount) {
        requirePolicy(policyId).recordBenefitPaid(amount);
    }
}
