package com.hospital.billing.claim.service;

import com.hospital.billing.claim.web.ClaimDtos.ClaimSummary;
import com.hospital.billing.common.event.BillFinalizedEvent;
import com.hospital.billing.common.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Finalising a bill automatically creates its claims: one Medicare (ECLIPSE) claim for the
 * Medicare benefit and one private fund claim for the fund benefit; the rest is the patient gap.
 *
 * <p>Runs AFTER the finalise transaction commits, in a NEW transaction, so a claim problem
 * (e.g. the fund policy lapsed, or nothing to claim for a self-funded patient) never undoes
 * the finalised bill and never fails the finalise request. The billing officer can still
 * create claims manually from the bill.</p>
 */
@Slf4j
@Component
public class ClaimAutoCreationListener {

    private final ClaimService claimService;
    private final TransactionTemplate newTransaction;

    public ClaimAutoCreationListener(ClaimService claimService, PlatformTransactionManager transactionManager) {
        this.claimService = claimService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillFinalized(BillFinalizedEvent event) {
        try {
            List<ClaimSummary> claims = newTransaction.execute(status -> claimService.createClaimsForBill(event.billId()));
            log.info("Created {} claim(s) automatically for bill {}", claims == null ? 0 : claims.size(), event.billNo());
        } catch (DomainException ex) {
            log.info("No claims created automatically for bill {}: {}", event.billNo(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Automatic claim creation failed for bill {}; create them from the bill page", event.billNo(), ex);
        }
    }
}
