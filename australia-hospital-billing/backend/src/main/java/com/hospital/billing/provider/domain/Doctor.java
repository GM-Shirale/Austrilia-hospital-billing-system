package com.hospital.billing.provider.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctor")
public class Doctor extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(nullable = false, length = 100)
    private String specialization;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    /** Medicare provider number: required to bill any MBS item. */
    @Column(name = "provider_no", nullable = false, length = 10)
    private String providerNo;

    /** What the hospital charges for a consultation (may exceed the MBS schedule fee). */
    @Column(name = "consultation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "mbs_item_no", nullable = false, length = 10)
    private String mbsItemNo;

    /** Government schedule fee for the MBS item: Medicare pays 75% of this for inpatients. */
    @Column(name = "mbs_schedule_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal mbsScheduleFee;

    @Column(nullable = false)
    private boolean active = true;

    public String displayName() {
        return "Dr " + firstName + " " + lastName;
    }
}
