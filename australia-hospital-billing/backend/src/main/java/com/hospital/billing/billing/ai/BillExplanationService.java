package com.hospital.billing.billing.ai;

import com.hospital.billing.adjudication.CoverageProfile;
import com.hospital.billing.billing.ai.AiBillingDtos.BillExplanation;
import com.hospital.billing.billing.ai.AiBillingDtos.BillExplanationResponse;
import com.hospital.billing.billing.domain.BillItem;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.billing.service.BillingService;
import com.hospital.billing.billing.service.CoverageResolver;
import com.hospital.billing.common.ai.LanguageModelClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Explains a bill in plain English for the patient ("why do I owe $250?").
 * The prompt is DE-IDENTIFIED: no patient name, MRN or Medicare number is sent, only line
 * types, descriptions and amounts that the engine already calculated. The model explains
 * the numbers; it never calculates them.
 */
@Service
@RequiredArgsConstructor
public class BillExplanationService {

    private static final String SYSTEM_PROMPT = """
            Explain this Australian hospital bill to the patient in plain English (reading age ~12).
            Return: summary (2-3 sentences), keyPoints (3-6 short bullet points covering what Medicare paid,
            what the health fund paid, the excess and any gap), and whatYouOwe (one sentence with the amount).
            Use exactly the amounts given. Do not give medical or financial advice.
            """;

    private final BillingService billingService;
    private final CoverageResolver coverageResolver;
    private final LanguageModelClient languageModel;

    @Transactional(readOnly = true)
    public BillExplanationResponse explain(Long billId) {
        HospitalBill bill = billingService.requireBill(billId);
        CoverageProfile coverage = coverageResolver.resolve(bill.getAdmission());
        String facts = facts(bill, coverage);

        return languageModel.ask(SYSTEM_PROMPT, facts, BillExplanation.class)
                .filter(e -> e.summary() != null && !e.summary().isBlank())
                .map(e -> new BillExplanationResponse("AI", languageModel.modelName(), e.summary(),
                        e.keyPoints() == null ? List.of() : e.keyPoints(), e.whatYouOwe(), AiBillingDtos.DISCLAIMER))
                .orElseGet(() -> template(bill, coverage));
    }

    static String facts(HospitalBill bill, CoverageProfile coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Patient class: ").append(coverage.financialClass()).append('\n');
        sb.append("Medicare eligible: ").append(coverage.medicareEligible()).append('\n');
        if (coverage.hasPrivateCover()) {
            var cover = coverage.privateCover();
            sb.append("Health fund: ").append(cover.fundName()).append(", ").append(cover.coverTier())
                    .append(" cover, hospital agreement ").append(cover.networkStatus())
                    .append(", excess $").append(cover.excessAmount()).append('\n');
        } else {
            sb.append("Health fund: none\n");
        }
        sb.append("Lines:\n");
        for (BillItem item : bill.getItems()) {
            sb.append("- ").append(item.getItemType()).append(": ").append(item.getDescription())
                    .append(" | charged $").append(item.getNetAmount())
                    .append(" | Medicare $").append(item.getMedicareBenefit())
                    .append(" | fund $").append(item.getInsuranceCoveredAmount())
                    .append(" | patient $").append(item.getPatientAmount()).append('\n');
        }
        sb.append("Totals: charged $").append(bill.getNetAmount())
                .append(", Medicare $").append(bill.getMedicareAmount())
                .append(", fund $").append(bill.getInsuranceAmount())
                .append(", patient payable $").append(bill.getPatientPayableAmount())
                .append(", already paid $").append(bill.getPaidAmount());
        return sb.toString();
    }

    private BillExplanationResponse template(HospitalBill bill, CoverageProfile coverage) {
        List<String> points = new ArrayList<>();
        points.add("Total charges for your stay: $" + bill.getNetAmount() + ".");
        if (bill.getMedicareAmount().signum() > 0) {
            points.add("Medicare covers $" + bill.getMedicareAmount() + " (75% of the government schedule fee for doctors and tests).");
        }
        if (coverage.hasPrivateCover()) {
            points.add(coverage.privateCover().fundName() + " covers $" + bill.getInsuranceAmount()
                    + " under your " + coverage.privateCover().coverTier() + " policy.");
            if (coverage.privateCover().excessAmount().signum() > 0) {
                points.add("Your policy excess of $" + coverage.privateCover().excessAmount() + " is included in your share.");
            }
        } else {
            points.add("You have no private health cover recorded for this admission.");
        }
        String owe = "You owe $" + bill.getPatientPayableAmount() + " in total"
                + (bill.getPaidAmount().signum() > 0 ? "; payments received so far: $" + bill.getPaidAmount() + "." : ".");
        String summary = "This bill covers your hospital stay, doctors, tests and medicines. "
                + "Medicare and your health fund pay part of it, and the rest is your out-of-pocket cost.";
        return new BillExplanationResponse("RULES", "template", summary, points, owe, AiBillingDtos.DISCLAIMER);
    }
}
