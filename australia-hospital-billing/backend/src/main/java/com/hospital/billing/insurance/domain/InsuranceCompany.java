package com.hospital.billing.insurance.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A payer contracted with the hospital (tenant-scoped: each hospital has its own contracts). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "insurance_company")
public class InsuranceCompany extends BaseTenantEntity {

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payer_type", nullable = false, length = 20)
    private PayerType payerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_status", nullable = false, length = 20)
    private NetworkStatus networkStatus;

    @Column(name = "known_gap_cap", precision = 12, scale = 2)
    private BigDecimal knownGapCap;

    @Column(length = 20)
    private String abn;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 200)
    private String address;
}
