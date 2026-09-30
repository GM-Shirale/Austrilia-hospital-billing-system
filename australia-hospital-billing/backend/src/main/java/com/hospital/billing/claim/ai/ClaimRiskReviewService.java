package com.hospital.billing.claim.ai;

import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.claim.domain.ClaimItem;
import com.hospital.billing.claim.domain.ClaimStatus;
import com.hospital.billing.claim.domain.InsuranceClaim;
import com.hospital.billing.claim.domain.InsuranceClaimRepository;
import com.hospital.billing.claim.service.ClaimValidator;
import com.hospital.billing.common.ai.LanguageModelClient;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.PayerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pre-submission risk review of a claim ("will this be denied, and why?").
 *
 * <p>Deterministic checks always run first (ClaimValidator + payer heuristics). The model only
 * adds narrative and recommendations on top: it can never lower the risk level or hide a
 * failed hard rule ("AI assists, rules decide").</p>
 */
@Service
@RequiredArgsConstructor
public class ClaimRiskReviewService {

    static final String DISCLAIMER = "Advisory review. Final lodgement decisions rest with the billing officer.";

    private static final String SYSTEM_PROMPT = """
            You review Australian hospital insurance claims (Medicare ECLIPSE and private health funds)
            before lodgement. Given the claim facts and the automated check results, return:
            riskLevel (LOW, MEDIUM or HIGH that the claim is rejected or partially paid), issues (short list),
            recommendations (short, actionable list for the billing officer) and a one-sentence summary.
            """;

    private final InsuranceClaimRepository claimRepository;
    private final ClaimValidator claimValidator;
    private final LanguageModelClient languageModel;

    /** Structured output expected from the model. */
    public record ClaimRiskReview(String riskLevel, List<String> issues, List<String> recommendations, String summary) {
    }

    public record ClaimReviewResponse(String source, String model, String riskLevel, String summary,
                                      List<String> issues, List<String> recommendations, String disclaimer) {
    }

    @Transactional(readOnly = true)
    public ClaimReviewResponse review(Long claimId) {
        InsuranceClaim claim = claimRepository.findWithItemsById(claimId)
                .orElseThrow(() -> new EntityNotFoundException("Claim", claimId));
        List<String> hardViolations = claim.getStatus() == ClaimStatus.DRAFT || claim.getStatus() == ClaimStatus.VALIDATED
                ? claimValidator.validate(claim) : List.of();
        List<String> heuristics = heuristicIssues(claim);
        String ruleRisk = !hardViolations.isEmpty() ? "HIGH" : heuristics.isEmpty() ? "LOW" : "MEDIUM";

        List<String> allRuleIssues = new ArrayList<>(hardViolations);
        allRuleIssues.addAll(heuristics);

        return languageModel.ask(SYSTEM_PROMPT, facts(claim, hardViolations, heuristics), ClaimRiskReview.class)
                .filter(r -> r.summary() != null)
                .map(r -> new ClaimReviewResponse("AI", languageModel.modelName(),
                        higherRisk(ruleRisk, r.riskLevel()), r.summary(),
                        merge(allRuleIssues, r.issues()), r.recommendations() == null ? List.of() : r.recommendations(),
                        DISCLAIMER))
                .orElseGet(() -> new ClaimReviewResponse("RULES", "rules", ruleRisk, ruleSummary(ruleRisk),
                        allRuleIssues, recommendations(claim, hardViolations, heuristics), DISCLAIMER));
    }

    private static List<String> heuristicIssues(InsuranceClaim claim) {
        List<String> issues = new ArrayList<>();
        if (claim.getPayerType() == PayerType.PRIVATE_FUND) {
            for (ClaimItem item : claim.getItems()) {
                BillItemType type = item.getBillItem().getItemType();
                if (type == BillItemType.EQUIPMENT || type == BillItemType.OTHER) {
                    issues.add("'%s' (%s) is often excluded from hospital products".formatted(
                            item.getBillItem().getDescription(), type));
                }
            }
            InsurancePolicy policy = claim.getPolicy();
            if (policy != null && policy.remainingAnnualLimit().compareTo(
                    claim.getClaimedAmount().multiply(new BigDecimal("1.2"))) < 0) {
                issues.add("Claim uses most of the member's remaining annual limit ($%s left)"
                        .formatted(policy.remainingAnnualLimit()));
            }
        }
        if (claim.getRejectionReason() != null) {
            issues.add("Payer response: " + claim.getRejectionReason());
        }
        return issues;
    }

    private static List<String> recommendations(InsuranceClaim claim, List<String> hard, List<String> heuristics) {
        List<String> recommendations = new ArrayList<>();
        if (!hard.isEmpty()) {
            recommendations.add("Fix the failed pre-submission checks, then run Validate again.");
        }
        if (heuristics.stream().anyMatch(i -> i.contains("excluded"))) {
            recommendations.add("Confirm the member's product covers these items, or tell the patient they may be out-of-pocket.");
        }
        if (heuristics.stream().anyMatch(i -> i.contains("annual limit"))) {
            recommendations.add("Check the benefit balance with the fund before lodging.");
        }
        if (claim.getStatus() == ClaimStatus.REJECTED) {
            recommendations.add("Invoice the patient for the rejected amount or lodge an appeal with supporting documents.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("No blocking issues found - the claim can be lodged.");
        }
        return recommendations;
    }

    private static String facts(InsuranceClaim claim, List<String> hard, List<String> heuristics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Payer type: ").append(claim.getPayerType()).append(", payer: ").append(claim.getCompany().getCompanyName())
                .append(", hospital agreement: ").append(claim.getCompany().getNetworkStatus()).append('\n');
        sb.append("Patient class: ").append(claim.getAdmission().getFinancialClass()).append('\n');
        if (claim.getPolicy() != null) {
            sb.append("Policy tier: ").append(claim.getPolicy().getCoverTier())
                    .append(", status ").append(claim.getPolicy().getStatus())
                    .append(", remaining annual limit $").append(claim.getPolicy().remainingAnnualLimit()).append('\n');
        }
        sb.append("Claim status: ").append(claim.getStatus()).append(", claimed $").append(claim.getClaimedAmount()).append('\n');
        sb.append("Lines:\n");
        for (ClaimItem item : claim.getItems()) {
            sb.append("- ").append(item.getBillItem().getItemType()).append(" | ").append(item.getBillItem().getDescription())
                    .append(" | MBS ").append(item.getBillItem().getMbsItemNo() == null ? "none" : item.getBillItem().getMbsItemNo())
                    .append(" | claimed $").append(item.getClaimedAmount()).append('\n');
        }
        sb.append("Failed automated checks: ").append(hard.isEmpty() ? "none" : String.join("; ", hard)).append('\n');
        sb.append("Heuristic warnings: ").append(heuristics.isEmpty() ? "none" : String.join("; ", heuristics));
        return sb.toString();
    }

    /** The AI may raise the risk level but never lower what the rules decided. */
    static String higherRisk(String ruleRisk, String aiRisk) {
        List<String> order = List.of("LOW", "MEDIUM", "HIGH");
        int rule = order.indexOf(ruleRisk);
        int ai = aiRisk == null ? -1 : order.indexOf(aiRisk.trim().toUpperCase());
        return order.get(Math.max(rule, ai));
    }

    private static List<String> merge(List<String> ruleIssues, List<String> aiIssues) {
        Set<String> merged = new LinkedHashSet<>(ruleIssues);
        if (aiIssues != null) {
            merged.addAll(aiIssues);
        }
        return List.copyOf(merged);
    }

    private static String ruleSummary(String risk) {
        return switch (risk) {
            case "HIGH" -> "The claim will be rejected as it stands: required information is missing or invalid.";
            case "MEDIUM" -> "The claim can be lodged, but parts of it may be reduced or rejected by the payer.";
            default -> "No issues found: the claim is ready to lodge.";
        };
    }
}
