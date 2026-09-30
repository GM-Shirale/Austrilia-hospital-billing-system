package com.hospital.billing.claim.event;

import com.hospital.billing.claim.service.ClaimWorkflowProcessor;
import com.hospital.billing.notification.NotificationDispatcher;
import com.hospital.billing.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Shared by the Kafka consumer and the in-memory listener: binds the event's tenant to the
 * thread BEFORE any transaction starts (Hibernate resolves the tenant when the session
 * opens), then routes to the right workflow step. The tenant is always cleared afterwards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimEventRouter {

    private final ClaimWorkflowProcessor processor;
    private final NotificationDispatcher notificationDispatcher;

    public void route(ClaimEvent event) {
        log.debug("Routing {} {} for tenant {}", event.type(), event.reference(), event.tenantId());
        TenantContext.runAs(event.tenantId(), () -> {
            switch (event.type()) {
                case CLAIM_SUBMITTED -> processor.handleSubmitted(event);
                case CLAIM_ADJUDICATED -> processor.handleAdjudicated(event);
                case REMITTANCE_POSTED -> processor.handleRemittance(event);
                case CLAIM_REJECTED -> notificationDispatcher.routeDenial(event);
                case NOTIFICATION -> notificationDispatcher.dispatch(event);
            }
        });
    }
}
