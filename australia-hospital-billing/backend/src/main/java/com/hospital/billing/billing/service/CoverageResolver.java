package com.hospital.billing.billing.service;

import com.hospital.billing.adjudication.CoverageProfile;
import com.hospital.billing.adjudication.CoverageProfile.PrivateCover;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.insurance.domain.InsuranceCompany;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.service.InsuranceService;
import com.hospital.billing.patient.domain.Patient;
import com.hospital.billing.patient.validation.MedicareNumberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Translates JPA state (patient, policy, fund contract) into the engine's CoverageProfile. */
@Component
@RequiredArgsConstructor
public class CoverageResolver {

    private final InsuranceService insuranceService;

    public CoverageProfile resolve(Admission admission) {
        Patient patient = admission.getPatient();
        boolean medicareEligible = patient.hasMedicare() && MedicareNumberValidator.isValid(patient.getMedicareNo());
        PrivateCover privateCover = insuranceService
                .findActivePrivatePolicy(patient.getId(), admission.getAdmissionDate().toLocalDate())
                .map(CoverageResolver::toPrivateCover)
                .orElse(null);
        return new CoverageProfile(admission.getFinancialClass(), admission.getCareSetting(), medicareEligible,
                privateCover);
    }

    private static PrivateCover toPrivateCover(InsurancePolicy policy) {
        InsuranceCompany fund = policy.getCompany();
        return new PrivateCover(policy.getId(), fund.getId(), fund.getCompanyName(), policy.getPolicyNo(),
                policy.getCoverTier(), fund.getNetworkStatus(), fund.getKnownGapCap(), policy.getExcessAmount(),
                policy.remainingAnnualLimit());
    }
}
