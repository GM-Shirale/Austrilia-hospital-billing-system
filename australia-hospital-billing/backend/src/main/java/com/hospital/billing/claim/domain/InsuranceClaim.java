package com.hospital.billing.claim.domain;

import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.billing.domain.HospitalBill;
import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.InvalidStateTransitionException;
import com.hospital.billing.common.money.Money;
import com.hospital.billing.insurance.domain.InsuranceCompany;
import com.hospital.billing.insurance.domain.InsurancePolicy;
import com.hospital.billing.insurance.domain.PayerType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "insurance_claim")
public class InsuranceClaim extends BaseTenantEntity {

    @Column(name = "claim_no", nullable = false, length = 30, updatable = false)
    private String claimNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_bill_id", nullable = false)
    private HospitalBill bill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private InsuranceCompany company;

    /** Null for Medicare claims (Medicare uses the patient's card, not a policy). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private InsurancePolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "payer_type", nullable = false, length = 20)
    private PayerType payerType;

    @Column(name = "authorization_no", length = 40)
    private String authorizationNo;

    @Column(name = "claim_date", nullable = false)
    private LocalDateTime claimDate;

    @Column(name = "claimed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal claimedAmount = Money.ZERO;

    @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal approvedAmount = Money.ZERO;

    @Column(name = "rejected_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal rejectedAmount = Money.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = Money.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Column(name = "payer_reference", length = 60)
    private String payerReference;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "adjudicated_at")
    private LocalDateTime adjudicatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ClaimItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ClaimStatusHistory> history = new ArrayList<>();

    public void addItem(ClaimItem item) {
        item.setClaim(this);
        items.add(item);
        claimedAmount = Money.of(claimedAmount.add(item.getClaimedAmount()));
    }

    /** The only way to change status: validates the transition and records an audit entry. */
    public void transitionTo(ClaimStatus target, String note, LocalDateTime at) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Claim " + claimNo, status, target);
        }
        ClaimStatusHistory entry = new ClaimStatusHistory();
        entry.setClaim(this);
        entry.setFromStatus(status);
        entry.setToStatus(target);
        entry.setNote(note);
        entry.setChangedAt(at);
        history.add(entry);
        status = target;
    }

    public void recordCreation(LocalDateTime at) {
        ClaimStatusHistory entry = new ClaimStatusHistory();
        entry.setClaim(this);
        entry.setToStatus(ClaimStatus.DRAFT);
        entry.setNote("Claim created from bill " + bill.getBillNo());
        entry.setChangedAt(at);
        history.add(entry);
    }

    public void applyDecision(BigDecimal approved, BigDecimal rejected, String reason, LocalDateTime at) {
        this.approvedAmount = Money.of(approved);
        this.rejectedAmount = Money.of(rejected);
        this.rejectionReason = reason;
        this.adjudicatedAt = at;
    }
}
