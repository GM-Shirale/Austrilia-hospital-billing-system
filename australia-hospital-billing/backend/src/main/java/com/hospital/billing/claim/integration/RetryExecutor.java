package com.hospital.billing.claim.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Exponential back-off for payer calls: waits initial, initial*2, initial*4 ... between
 * attempts, only for {@link PayerCommunicationException} (transient). Business errors are
 * never retried.
 */
@Slf4j
@Component
public class RetryExecutor {

    private final int maxAttempts;
    private final long initialDelayMs;
    private final double multiplier;

    public RetryExecutor(@Value("${app.claims.retry.max-attempts:4}") int maxAttempts,
                         @Value("${app.claims.retry.initial-delay-ms:500}") long initialDelayMs,
                         @Value("${app.claims.retry.multiplier:2.0}") double multiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
    }

    public <T> T execute(String operation, Supplier<T> action) {
        long delay = initialDelayMs;
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (PayerCommunicationException ex) {
                if (attempt >= maxAttempts) {
                    log.error("{} failed after {} attempts: {}", operation, attempt, ex.getMessage());
                    throw ex;
                }
                log.warn("{} failed (attempt {}/{}): {}. Retrying in {} ms", operation, attempt, maxAttempts,
                        ex.getMessage(), delay);
                sleep(delay);
                delay = (long) (delay * multiplier);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PayerCommunicationException("Interrupted during retry back-off", e);
        }
    }
}
