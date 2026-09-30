package com.hospital.billing.notification;

import com.hospital.billing.claim.event.ClaimEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumer of notification.dispatch and claim.rejected. In this project it logs the message;
 * plugging in e-mail (Spring Mail / SES) or SMS (Twilio / MessageMedia) means implementing
 * one method, with no change to the producers.
 */
@Slf4j
@Component
public class NotificationDispatcher {

    public void dispatch(ClaimEvent event) {
        log.info("[NOTIFY] tenant={} ref={} amount={} :: {}", event.tenantId(), event.reference(), event.amount(),
                event.message());
    }

    /** Denial routing: rejected claims go to the billing team's work queue and the patient is told. */
    public void routeDenial(ClaimEvent event) {
        log.warn("[DENIAL QUEUE] claim {} rejected ({}), amount {} is now payable by the patient",
                event.reference(), event.message(), event.amount());
        dispatch(event);
    }
}
