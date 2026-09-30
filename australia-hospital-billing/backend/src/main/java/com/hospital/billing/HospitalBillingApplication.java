package com.hospital.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point of the Australia Hospital Billing System.
 *
 * <p>Architecture: a <b>modular monolith</b>. Each top-level package (patient, provider,
 * admission, lab, pharmacy, insurance, billing, claim) is a bounded context that talks to
 * the others through service interfaces, and to asynchronous workflows through events
 * (Kafka, or in-memory Spring events when Kafka is not running).</p>
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@ConfigurationPropertiesScan
public class HospitalBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalBillingApplication.class, args);
    }
}
