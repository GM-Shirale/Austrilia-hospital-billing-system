package com.hospital.billing.adjudication;

import com.hospital.billing.adjudication.CoverageProfile.PrivateCover;
import com.hospital.billing.admission.domain.FinancialClass;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.PayerType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Private health insurance rules (simplified but structurally faithful):
 * <ol>
 *   <li><b>Medical services with an MBS fee:</b> the fund pays the remaining 25% of the
 *       schedule fee. Charges ABOVE the schedule fee depend on the hospital's agreement with
 *       the fund: NO_GAP = fund pays all of it; KNOWN_GAP = patient pays at most the known-gap
 *       cap; NON_PARTICIPATING = patient pays all of it.</li>
 *   <li><b>Hospital charges</b> (accommodation, nursing, equipment): fund pays the tier's
 *       hospital cover percentage.</li>
 *   <li><b>Pharmacy</b>: fund pays the tier's pharmacy percentage.</li>
 *   <li><b>OTHER</b> (non-clinical extras): not covered.</li>
 *   <li>The policy <b>excess</b> is deducted from the benefit (hospital charges first).</li>
 *   <li>The total is capped at the policy's remaining <b>annual limit</b> (benefit accumulator).</li>
 * </ol>
 */
@Component
@Order(2)
public class PrivateFundAdjudicator implements ClaimAdjudicatorStrategy {

    public static final BigDecimal SCHEDULE_FEE_GAP_RATE = new BigDecimal("0.25");

    @Override
    public PayerType payerType() {
        return PayerType.PRIVATE_FUND;
    }

    @Override
    public boolean supports(CoverageProfile coverage) {
        // Australian funds may not pay for out-of-hospital (OPD) medical services: Medicare only.
        return coverage.hasPrivateCover() && coverage.financialClass() == FinancialClass.PRIVATE
                && !coverage.outpatient();
    }

    @Override
    public PayerAdjudication adjudicate(List<AdjudicationLine> lines, CoverageProfile coverage,
                                        Map<Long, BigDecimal> remainingByLine) {
        PrivateCover cover = coverage.privateCover();
        Map<Long, BigDecimal> benefit = new LinkedHashMap<>();
        Map<Long, String> lineNotes = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();

        for (AdjudicationLine line : lines) {
            BigDecimal remaining = remainingByLine.getOrDefault(line.billItemId(), Money.ZERO);
            if (remaining.signum() <= 0) {
                continue;
            }
            BigDecimal amount = Money.min(remaining, grossBenefit(line, cover, remaining, lineNotes));
            if (amount.signum() > 0) {
                benefit.put(line.billItemId(), amount);
            }
        }

        applyExcess(lines, cover, benefit, notes);
        applyAnnualLimit(lines, cover, benefit, notes);

        notes.add(0, "%s %s cover (%s agreement), policy %s".formatted(cover.fundName(), cover.coverTier(),
                cover.networkStatus(), cover.policyNo()));
        List<LineBenefit> result = benefit.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .map(e -> new LineBenefit(e.getKey(), e.getValue(), lineNotes.get(e.getKey())))
                .toList();
        return new PayerAdjudication(PayerType.PRIVATE_FUND, result, notes);
    }

    private BigDecimal grossBenefit(AdjudicationLine line, PrivateCover cover, BigDecimal remaining,
                                    Map<Long, String> lineNotes) {
        BillItemType type = line.itemType();
        if (type.isMedicalService() && line.hasScheduleFee()) {
            BigDecimal fee = line.scheduleFeeTotal();
            BigDecimal gapTopUp = Money.percentOf(fee, SCHEDULE_FEE_GAP_RATE);
            BigDecimal aboveSchedule = Money.nonNegative(line.netAmount().subtract(fee));
            BigDecimal cap = cover.knownGapCap() == null ? Money.ZERO : cover.knownGapCap();
            BigDecimal aboveScheduleCovered = switch (cover.networkStatus()) {
                case NO_GAP -> aboveSchedule;
                case KNOWN_GAP -> Money.nonNegative(aboveSchedule.subtract(cap));
                case NON_PARTICIPATING, NOT_APPLICABLE -> Money.ZERO;
            };
            lineNotes.put(line.billItemId(), "25%% of schedule fee + %s of %s above schedule"
                    .formatted(aboveScheduleCovered, aboveSchedule));
            return Money.of(gapTopUp.add(aboveScheduleCovered));
        }
        if (type.isHospitalCharge() || type.isMedicalService()) {
            lineNotes.put(line.billItemId(), "Hospital cover " + cover.coverTier().hospitalChargeCover());
            return Money.percentOf(remaining, cover.coverTier().hospitalChargeCover());
        }
        if (type == BillItemType.PHARMACY) {
            lineNotes.put(line.billItemId(), "Pharmacy cover " + cover.coverTier().pharmacyCover());
            return Money.percentOf(remaining, cover.coverTier().pharmacyCover());
        }
        return Money.ZERO;
    }

    /** Excess is paid by the patient, taken from hospital charges first. */
    private void applyExcess(List<AdjudicationLine> lines, PrivateCover cover, Map<Long, BigDecimal> benefit,
                             List<String> notes) {
        BigDecimal excessLeft = Money.of(cover.excessAmount());
        if (excessLeft.signum() <= 0) {
            return;
        }
        List<AdjudicationLine> ordered = lines.stream()
                .sorted(Comparator.comparing((AdjudicationLine l) -> !l.itemType().isHospitalCharge()))
                .toList();
        BigDecimal applied = Money.ZERO;
        for (AdjudicationLine line : ordered) {
            if (excessLeft.signum() <= 0) {
                break;
            }
            BigDecimal current = benefit.getOrDefault(line.billItemId(), Money.ZERO);
            BigDecimal deduction = Money.min(current, excessLeft);
            if (deduction.signum() > 0) {
                benefit.put(line.billItemId(), Money.of(current.subtract(deduction)));
                excessLeft = Money.of(excessLeft.subtract(deduction));
                applied = Money.of(applied.add(deduction));
            }
        }
        notes.add("Policy excess applied: " + applied);
    }

    /** Benefit accumulator: never pay more than what is left of the annual limit. */
    private void applyAnnualLimit(List<AdjudicationLine> lines, PrivateCover cover, Map<Long, BigDecimal> benefit,
                                  List<String> notes) {
        BigDecimal total = Money.of(benefit.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal limit = Money.of(cover.remainingAnnualLimit());
        if (total.compareTo(limit) <= 0) {
            return;
        }
        BigDecimal overflow = Money.of(total.subtract(limit));
        for (int i = lines.size() - 1; i >= 0 && overflow.signum() > 0; i--) {
            Long id = lines.get(i).billItemId();
            BigDecimal current = benefit.getOrDefault(id, Money.ZERO);
            BigDecimal cut = Money.min(current, overflow);
            benefit.put(id, Money.of(current.subtract(cut)));
            overflow = Money.of(overflow.subtract(cut));
        }
        notes.add("Benefit capped at remaining annual limit " + limit);
    }
}
