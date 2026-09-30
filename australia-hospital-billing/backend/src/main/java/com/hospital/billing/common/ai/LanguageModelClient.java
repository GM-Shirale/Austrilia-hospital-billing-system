package com.hospital.billing.common.ai;

import java.util.Optional;

/**
 * Dependency Inversion for AI: business modules depend on this small contract, not on
 * Spring AI directly. That keeps them unit-testable and lets the model provider change
 * (OpenAI, Azure OpenAI, Ollama...) without touching billing or claims code.
 *
 * <p>Contract: never throws. When AI is disabled, unreachable, or returns something that
 * cannot be parsed, the result is empty and the caller uses its deterministic fallback.</p>
 */
public interface LanguageModelClient {

    boolean isEnabled();

    String modelName();

    /** Asks the model and maps the answer onto {@code responseType} (structured output). */
    <T> Optional<T> ask(String systemPrompt, String userPrompt, Class<T> responseType);
}
