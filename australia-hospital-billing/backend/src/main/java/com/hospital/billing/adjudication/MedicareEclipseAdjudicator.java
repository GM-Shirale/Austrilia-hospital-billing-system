package com.hospital.billing.adjudication;

import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.PayerType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Medicare rules (claimed electronically through ECLIPSE):
 * <ul>
 *   <li>PRIVATE in-patient (IPD): Medicare pays 75% of the MBS schedule fee for each MBS
 *       service (doctor, procedure, pathology).</li>
 *   <li>PRIVATE out-patient (OPD): Medicare pays 85% of the MBS schedule fee.</li>
 *   <li>Accommodation (room), pharmacy and extras attract NO Medicare benefit (0%).</li>
 *   <li>PUBLIC patient: treatment is fully funded under the public hospital agreement, so
 *       nothing is left for the patient to pay.</li>
 * </ul>
 */
@Component
@Order(1)
public class MedicareEclipseAdjudicator implements ClaimAdjudicatorStrategy {

    public static final BigDecimal INPATIENT_BENEFIT_RATE = new BigDecimal("0.75");
    public static final BigDecimal OUTPATIENT_BENEFIT_RATE = new BigDecimal("0.85");

    /** 75% for admitted patients, 85% for out-patient services. */
    public static BigDecimal benefitRate(CoverageProfile coverage) {
        return coverage.outpatient() ? OUTPATIENT_BENEFIT_RATE : INPATIENT_BENEFIT_RATE;
    }

    @Override
    public PayerType payerType() {
        return PayerType.MEDICARE;
    }

    @Override
    public boolean supports(CoverageProfile coverage) {
        return coverage.medicareEligible();
    }

    @Override
    public PayerAdjudication adjudicate(List<AdjudicationLine> lines, CoverageProfile coverage,
                                        Map<Long, BigDecimal> remainingByLine) {
        List<LineBenefit> benefits = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        if (coverage.financialClass() == FinancialClass.PUBLIC) {
            for (AdjudicationLine line : lines) {
                BigDecimal remaining = remainingByLine.getOrDefault(line.billItemId(), Money.ZERO);
                benefits.add(new LineBenefit(line.billItemId(), remaining, "Public patient - fully funded"));
            }
            notes.add("Public patient: treatment funded under the public hospital agreement, no patient charge");
            return new PayerAdjudication(PayerType.MEDICARE, benefits, notes);
        }

        BigDecimal rate = benefitRate(coverage);
        String percent = rate.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
        String setting = coverage.outpatient() ? "out-patient (OPD)" : "in-patient (IPD)";
        for (AdjudicationLine line : lines) {
            if (!line.itemType().isMedicalService() || !line.hasScheduleFee()) {
                continue;               // room, pharmacy, extras: 0% Medicare
            }
            BigDecimal remaining = remainingByLine.getOrDefault(line.billItemId(), Money.ZERO);
            BigDecimal benefit = Money.min(remaining, Money.percentOf(line.scheduleFeeTotal(), rate));
            if (benefit.signum() > 0) {
                benefits.add(new LineBenefit(line.billItemId(), benefit,
                        percent + " of MBS schedule fee " + Money.of(line.scheduleFeeTotal())));
            }
        }
        notes.add("Medicare: %s of the MBS schedule fee for %s medical services; no rebate on accommodation or pharmacy"
                .formatted(percent, setting));
        return new PayerAdjudication(PayerType.MEDICARE, benefits, notes);
    }
}
