package com.hospital.billing.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Business dates are recorded in Australian local time (ER diagram note 5).
 * Injecting a Clock (instead of calling LocalDateTime.now()) also makes time testable.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.zone-id:Australia/Sydney}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
