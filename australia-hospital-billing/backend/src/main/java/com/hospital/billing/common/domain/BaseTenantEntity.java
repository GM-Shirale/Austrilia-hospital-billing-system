package com.hospital.billing.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base class for every tenant-owned (hospital-owned) table.
 *
 * <p>{@link TenantId} is Hibernate 6's discriminator-based multi-tenancy:</p>
 * <ul>
 *   <li>on INSERT, Hibernate fills {@code tenant_id} from the current tenant;</li>
 *   <li>on every SELECT / UPDATE / DELETE (JPQL, Criteria, derived queries, lazy loads),
 *       Hibernate appends {@code WHERE tenant_id = :currentTenant} automatically.</li>
 * </ul>
 * <p>The current tenant comes from
 * {@link com.hospital.billing.tenant.TenantIdentifierResolver}, which reads
 * {@link com.hospital.billing.tenant.TenantContext}. Repositories and controllers never
 * mention the tenant, so a developer cannot "forget" the filter.</p>
 */
@Getter
@MappedSuperclass
public abstract class BaseTenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
