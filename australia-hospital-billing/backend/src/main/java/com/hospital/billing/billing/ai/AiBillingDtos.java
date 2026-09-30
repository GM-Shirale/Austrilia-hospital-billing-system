package com.hospital.billing.billing.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Request/response and structured-output types for the billing AI features. */
public final class AiBillingDtos {

    public static final String DISCLAIMER =
            "AI-generated suggestion. A qualified clinical coder or billing officer must verify it before use.";

    private AiBillingDtos() {
    }

    public record MbsSuggestionRequest(
            @NotBlank @Size(max = 2000) String clinicalNotes,
            @Size(max = 100) String specialty) {
    }

    /** One suggested MBS item. Also the shape the model must return (structured output). */
    public record MbsSuggestion(String mbsItemNo, String description, String itemType,
                                BigDecimal indicativeScheduleFee, String rationale, Double confidence) {
    }

    /** Wrapper so the model returns {"suggestions":[...]} (structured output needs an object root). */
    public record MbsSuggestionList(List<MbsSuggestion> suggestions) {
    }

    public record MbsSuggestionResponse(String source, String model, List<MbsSuggestion> suggestions,
                                        String disclaimer) {
    }

    /** Structured output for the patient-friendly bill explanation. */
    public record BillExplanation(String summary, List<String> keyPoints, String whatYouOwe) {
    }

    public record BillExplanationResponse(String source, String model, String summary, List<String> keyPoints,
                                          String whatYouOwe, String disclaimer) {
    }
}
