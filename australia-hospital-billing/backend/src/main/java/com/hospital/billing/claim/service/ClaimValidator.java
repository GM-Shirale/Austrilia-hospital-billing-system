package com.hospital.billing.claim.service;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.billing.domain.BillItem;
import com.hospital.billing.billing.domain.BillStatus;
import com.hospital.billing.claim.domain.ClaimItem;
import com.hospital.billing.claim.domain.InsuranceClaim;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.PayerType;
import com.hospital.billing.patient.domain.Patient;
import com.hospital.billing.patient.validation.MedicareNumberValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-submission checks. Collects EVERY violation (instead of failing on the first), so
 * billing staff can fix all problems in one go. Catching these before lodging avoids
 * weeks-long rejection cycles with the payer.
 */
@Component
public class ClaimValidator {

    public List<String> validate(InsuranceClaim claim) {
        List<String> violations = new ArrayList<>();
        BillStatus billStatus = claim.getBill().getStatus();
        if (billStatus == BillStatus.DRAFT || billStatus == BillStatus.CANCELLED) {
            violations.add("Bill %s is %s; only finalised bills can be claimed".formatted(claim.getBill().getBillNo(), billStatus));
        }
        if (claim.getItems().isEmpty() || claim.getClaimedAmount().signum() <= 0) {
            violations.add("Claim has no claimable lines");
        }
        if (claim.getPayerType() == PayerType.MEDICARE) {
            validateMedicare(claim, violations);
        } else {
            validatePrivateFund(claim, violations);
        }
        return violations;
    }

    private void validateMedicare(InsuranceClaim claim, List<String> violations) {
        Admission admission = claim.getAdmission();
        Patient patient = admission.getPatient();
        if (!MedicareNumberValidator.isValid(patient.getMedicareNo())) {
            violations.add("Patient Medicare card number is missing or invalid");
        }
        if (patient.getMedicareIrn() == null) {
            violations.add("Patient Medicare IRN (position on card) is missing");
        }
        String providerNo = admission.getDoctor().getProviderNo();
        if (providerNo == null || providerNo.isBlank()) {
            violations.add("Attending doctor has no Medicare provider number");
        }
        if (admission.getFinancialClass() == FinancialClass.PRIVATE) {
            for (ClaimItem item : claim.getItems()) {
                BillItem billItem = item.getBillItem();
                if (billItem.getMbsItemNo() == null || billItem.getMbsItemNo().isBlank()) {
                    violations.add("Line '%s' has no MBS item number".formatted(billItem.getDescription()));
                }
            }
        }
    }

    private void validatePrivateFund(InsuranceClaim claim, List<String> violations) {
        InsurancePolicy policy = claim.getPolicy();
        if (policy == null) {
            violations.add("Private fund claim has no policy");
            return;
        }
        if (!policy.isActiveOn(claim.getAdmission().getAdmissionDate().toLocalDate())) {
            violations.add("Policy %s was not active on the admission date".formatted(policy.getPolicyNo()));
        }
        if (claim.getClaimedAmount().compareTo(policy.remainingAnnualLimit()) > 0) {
            violations.add("Claimed %s exceeds the remaining annual limit of %s"
                    .formatted(claim.getClaimedAmount(), policy.remainingAnnualLimit()));
        }
    }
}
