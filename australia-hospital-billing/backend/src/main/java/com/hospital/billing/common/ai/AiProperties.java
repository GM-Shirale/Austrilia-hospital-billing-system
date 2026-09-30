package com.hospital.billing.common.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.ai.enabled=true turns on the Spring AI calls (AI_ENABLED env var). */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(boolean enabled) {
}
