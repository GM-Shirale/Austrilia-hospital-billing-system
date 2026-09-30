package com.hospital.billing.adjudication;

import com.hospital.billing.admission.domain.CareSetting;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.insurance.domain.CoverTier;
import com.hospital.billing.insurance.domain.NetworkStatus;

import java.math.BigDecimal;

/**
 * Everything the adjudicators need to know about the patient's cover, decoupled from JPA
 * entities so the engine is pure, deterministic and unit-testable.
 */
public record CoverageProfile(FinancialClass financialClass, CareSetting careSetting, boolean medicareEligible,
                              PrivateCover privateCover) {

    public CoverageProfile {
        careSetting = careSetting == null ? CareSetting.IPD : careSetting;
    }

    /** In-patient profile (kept for callers that predate out-patient support). */
    public CoverageProfile(FinancialClass financialClass, boolean medicareEligible, PrivateCover privateCover) {
        this(financialClass, CareSetting.IPD, medicareEligible, privateCover);
    }

    public boolean outpatient() {
        return careSetting == CareSetting.OPD;
    }

    public record PrivateCover(Long policyId,
                               Long companyId,
                               String fundName,
                               String policyNo,
                               CoverTier coverTier,
                               NetworkStatus networkStatus,
                               BigDecimal knownGapCap,
                               BigDecimal excessAmount,
                               BigDecimal remainingAnnualLimit) {
    }

    public boolean hasPrivateCover() {
        return privateCover != null;
    }
}
