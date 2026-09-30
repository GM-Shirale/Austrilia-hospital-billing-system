package com.hospital.billing.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Bridge between {@link TenantContext} and Hibernate's {@code @TenantId} support.
 *
 * <p>Hibernate calls {@link #resolveCurrentTenantIdentifier()} whenever it opens a session
 * (i.e. at the start of each transaction) and uses the value both to populate
 * {@code tenant_id} on insert and to filter every query.</p>
 *
 * <p>When no tenant is bound (login, public endpoints) we return {@link #ROOT_TENANT};
 * {@link #isRoot} tells Hibernate not to filter in that case. Only unauthenticated,
 * read-only endpoints run without a tenant.</p>
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    public static final UUID ROOT_TENANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.getCurrentTenant().orElse(ROOT_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return ROOT_TENANT.equals(tenantId);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
