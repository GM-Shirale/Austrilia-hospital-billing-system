package com.hospital.billing.billing.web;

import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionRequest;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionResponse;
import com.hospital.billing.billing.ai.MbsCodingAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI assistant")
@RestController
@RequestMapping("/api/v1/coding")
@RequiredArgsConstructor
public class ClinicalCodingController {

    private final MbsCodingAssistant mbsCodingAssistant;

    @Operation(summary = "Suggest MBS items from clinical notes (Spring AI, rule-based fallback)")
    @PostMapping("/mbs-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OFFICER','DOCTOR')")
    public MbsSuggestionResponse suggest(@Valid @RequestBody MbsSuggestionRequest request) {
        return mbsCodingAssistant.suggest(request);
    }
}
