package com.hospital.billing.claim.service;

import com.hospital.billing.claim.domain.ClaimItem;
import com.hospital.billing.claim.domain.ExplanationOfBenefit;
import com.hospital.billing.claim.domain.ExplanationOfBenefitRepository;
import com.hospital.billing.claim.domain.InsuranceClaim;
import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/** Generates the Explanation of Benefits once a payer's remittance is posted. Idempotent. */
@Service
@RequiredArgsConstructor
public class EobService {

    private final ExplanationOfBenefitRepository eobRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final Clock clock;

    @Transactional
    public ExplanationOfBenefit generate(InsuranceClaim claim) {
        return eobRepository.findByClaimId(claim.getId()).orElseGet(() -> create(claim));
    }

    private ExplanationOfBenefit create(InsuranceClaim claim) {
        BigDecimal charged = Money.ZERO;
        BigDecimal patientShare = Money.ZERO;
        int services = 0;
        for (ClaimItem item : claim.getItems()) {
            charged = charged.add(item.getBillItem().getNetAmount());
            patientShare = patientShare.add(item.getBillItem().getPatientAmount());
            services++;
        }
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setEobNo(documentNumberGenerator.next(DocumentType.EOB));
        eob.setClaim(claim);
        eob.setIssuedAt(LocalDateTime.now(clock));
        eob.setTotalCharged(Money.of(charged));
        eob.setTotalBenefit(Money.of(claim.getPaidAmount()));
        eob.setPatientResponsibility(Money.of(patientShare));
        eob.setSummary(("%s paid $%s towards %d service(s) on bill %s (charged $%s). "
                + "Your remaining responsibility for these services is $%s.%s").formatted(
                claim.getCompany().getCompanyName(), Money.of(claim.getPaidAmount()), services,
                claim.getBill().getBillNo(), Money.of(charged), Money.of(patientShare),
                claim.getRejectedAmount().signum() > 0
                        ? " Not approved: $" + claim.getRejectedAmount() + " (" + claim.getRejectionReason() + ")."
                        : ""));
        return eobRepository.save(eob);
    }
}
