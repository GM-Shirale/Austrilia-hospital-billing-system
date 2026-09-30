package com.hospital.billing.common.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI assistant")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiStatusController {

    private final LanguageModelClient languageModelClient;

    public record AiStatus(boolean enabled, String model) {
    }

    @Operation(summary = "Whether Spring AI is enabled, and which model is used")
    @GetMapping("/status")
    public AiStatus status() {
        return new AiStatus(languageModelClient.isEnabled(), languageModelClient.modelName());
    }
}
