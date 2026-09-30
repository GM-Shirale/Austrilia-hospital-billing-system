package com.hospital.billing.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Tenant registry: one row per hospital. Not tenant-scoped itself. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "hospital")
public class Hospital {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "hospital_type", nullable = false)
    private HospitalType hospitalType;

    private String abn;

    @Column(nullable = false)
    private String state;

    @Column(name = "address_line")
    private String addressLine;

    private String suburb;

    private String postcode;

    private String phone;

    private String email;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
