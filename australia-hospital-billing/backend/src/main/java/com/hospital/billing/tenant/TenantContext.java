package com.hospital.billing.tenant;

import com.hospital.billing.common.exception.TenantAccessDeniedException;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Holds the hospital (tenant) of the current unit of work.
 *
 * <p>Thread-safety: the value lives in a {@link ThreadLocal}, so every request thread sees
 * only its own tenant. Servlet containers REUSE threads, therefore whoever sets the tenant
 * must clear it in a {@code finally} block ({@link TenantFilter} for HTTP requests,
 * {@link #callAs}/{@link #runAs} for async consumers), otherwise the next request served by
 * that thread would inherit another hospital's tenant.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final String MDC_KEY = "tenantId";

    private TenantContext() {
    }

    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(Objects.requireNonNull(tenantId, "tenantId"));
        MDC.put(MDC_KEY, tenantId.toString());
    }

    public static Optional<UUID> getCurrentTenant() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static UUID requireCurrentTenant() {
        return getCurrentTenant()
                .orElseThrow(() -> new TenantAccessDeniedException("No hospital (tenant) is bound to this request"));
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        MDC.remove(MDC_KEY);
    }

    /** Runs an action as the given tenant and restores the previous state afterwards. */
    public static <T> T callAs(UUID tenantId, Supplier<T> action) {
        UUID previous = CURRENT_TENANT.get();
        setCurrentTenant(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                setCurrentTenant(previous);
            }
        }
    }

    public static void runAs(UUID tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
