package com.hospital.billing.common.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring AI implementation using the auto-configured {@link ChatClient}.
 * Structured output: {@code .call().entity(Type.class)} makes Spring AI add a JSON schema to
 * the prompt and convert the reply into a Java record.
 */
@Slf4j
@Component
public class SpringAiLanguageModelClient implements LanguageModelClient {

    /** Guardrails prepended to every system prompt. */
    static final String GUARDRAILS = """
            You assist the billing department of an Australian hospital.
            Rules: use only the facts provided; never invent patient details, amounts or policy terms;
            amounts are Australian dollars (AUD); use Australian English; be concise;
            your output is advisory and will be reviewed by a human before any action is taken.
            """;

    private final ChatClient chatClient;
    private final String model;

    public SpringAiLanguageModelClient(ObjectProvider<ChatClient.Builder> chatClientBuilder,
                                       AiProperties properties,
                                       @Value("${spring.ai.openai.chat.options.model:unknown}") String model) {
        ChatClient.Builder builder = properties.enabled() ? chatClientBuilder.getIfAvailable() : null;
        this.chatClient = builder == null ? null : builder.build();
        this.model = model;
        if (chatClient == null) {
            log.info("Spring AI is disabled (app.ai.enabled=false): AI features use rule-based fallbacks");
        } else {
            log.info("Spring AI is enabled with model {}", model);
        }
    }

    @Override
    public boolean isEnabled() {
        return chatClient != null;
    }

    @Override
    public String modelName() {
        return isEnabled() ? model : "rules";
    }

    @Override
    public <T> Optional<T> ask(String systemPrompt, String userPrompt, Class<T> responseType) {
        if (chatClient == null) {
            return Optional.empty();
        }
        long start = System.currentTimeMillis();
        try {
            T result = chatClient.prompt()
                    .system(GUARDRAILS + "\n" + systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(responseType);
            log.info("AI {} answered in {} ms", responseType.getSimpleName(), System.currentTimeMillis() - start);
            return Optional.ofNullable(result);
        } catch (RuntimeException ex) {
            log.warn("AI call for {} failed, using fallback: {}", responseType.getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }
}
