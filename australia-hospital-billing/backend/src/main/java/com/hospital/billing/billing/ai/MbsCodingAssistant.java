package com.hospital.billing.billing.ai;

import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestion;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionList;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionRequest;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionResponse;
import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.common.ai.LanguageModelClient;
import com.hospital.billing.common.ai.PiiRedactor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI clinical-coding assistant: suggests MBS item numbers from free-text clinical notes.
 *
 * <p>Flow: redact identifiers -> ask the model for structured output -> sanitise the answer
 * (max 5, valid item types, confidence 0..1). If AI is off or fails, a keyword rule table
 * answers instead, so the feature always works. Schedule fees are indicative only.</p>
 */
@Service
public class MbsCodingAssistant {

    private static final int MAX_SUGGESTIONS = 5;

    private static final String SYSTEM_PROMPT = """
            You are an experienced Australian clinical coder. From the clinical notes, suggest up to 5
            Medicare Benefits Schedule (MBS) items that could be billed for an admitted (in-hospital) patient.
            For each item give: mbsItemNo, a short description, itemType (one of CONSULTATION, DOCTOR,
            SURGERY, PROCEDURE, LAB), the indicative schedule fee in AUD, a one-sentence rationale that quotes
            the part of the notes supporting it, and a confidence between 0 and 1.
            Do not suggest items the notes do not support.
            """;

    /** Deterministic fallback: ordered keyword rules (illustrative fees). */
    private static final Map<Pattern, MbsSuggestion> RULES = new LinkedHashMap<>();

    static {
        rule("chest pain|troponin|cardiac enzyme", "66518", "Quantitation of cardiac troponin", "LAB", "29.05");
        rule("ecg|electrocardiogram", "11714", "12-lead electrocardiography, tracing and report", "PROCEDURE", "32.50");
        rule("chest x-?ray|cxr", "58500", "Chest radiography", "PROCEDURE", "47.15");
        rule("full blood count|fbc|anaemi", "65070", "Full blood count", "LAB", "16.95");
        rule("electrolyte|renal function|creatinine|uec", "66512", "Urea, electrolytes and creatinine", "LAB", "17.70");
        rule("appendic", "30571", "Appendicectomy", "SURGERY", "590.60");
        rule("cholecystectomy|gall ?bladder|gallstone", "30445", "Laparoscopic cholecystectomy", "SURGERY", "899.10");
        rule("knee arthroscop", "49557", "Arthroscopic surgery of the knee", "SURGERY", "668.35");
        rule("hip replacement|arthroplasty of hip", "49318", "Total hip replacement", "SURGERY", "1528.15");
        rule("colonoscopy", "32222", "Colonoscopy to the caecum", "PROCEDURE", "354.70");
        rule("cardiolog|cardiac review|heart failure", "110", "Consultant physician (cardiology) initial attendance", "CONSULTATION", "170.50");
        rule("specialist|surgeon review|pre-?op", "104", "Specialist initial attendance", "CONSULTATION", "98.95");
        rule("ward round|review|follow-?up|subsequent", "105", "Specialist subsequent attendance", "CONSULTATION", "49.80");
    }

    private final LanguageModelClient languageModel;

    public MbsCodingAssistant(LanguageModelClient languageModel) {
        this.languageModel = languageModel;
    }

    public MbsSuggestionResponse suggest(MbsSuggestionRequest request) {
        String notes = PiiRedactor.redact(request.clinicalNotes());
        String userPrompt = "Specialty: %s%nClinical notes:%n%s".formatted(
                request.specialty() == null || request.specialty().isBlank() ? "not stated" : request.specialty(), notes);

        List<MbsSuggestion> fromAi = languageModel.ask(SYSTEM_PROMPT, userPrompt, MbsSuggestionList.class)
                .map(MbsSuggestionList::suggestions)
                .map(MbsCodingAssistant::sanitise)
                .orElse(List.of());
        if (!fromAi.isEmpty()) {
            return new MbsSuggestionResponse("AI", languageModel.modelName(), fromAi, AiBillingDtos.DISCLAIMER);
        }
        return new MbsSuggestionResponse("RULES", "keyword rules", ruleBased(notes), AiBillingDtos.DISCLAIMER);
    }

    static List<MbsSuggestion> ruleBased(String notes) {
        String text = notes.toLowerCase();
        List<MbsSuggestion> matches = new ArrayList<>();
        RULES.forEach((pattern, suggestion) -> {
            if (matches.size() < MAX_SUGGESTIONS && pattern.matcher(text).find()) {
                matches.add(suggestion);
            }
        });
        return matches;
    }

    static List<MbsSuggestion> sanitise(List<MbsSuggestion> suggestions) {
        if (suggestions == null) {
            return List.of();
        }
        List<String> allowedTypes = Arrays.stream(BillItemType.values()).map(Enum::name).toList();
        return suggestions.stream()
                .filter(s -> s != null && s.mbsItemNo() != null && s.mbsItemNo().matches("\\d{1,6}"))
                .limit(MAX_SUGGESTIONS)
                .map(s -> new MbsSuggestion(
                        s.mbsItemNo(),
                        s.description(),
                        s.itemType() != null && allowedTypes.contains(s.itemType().toUpperCase())
                                ? s.itemType().toUpperCase() : "PROCEDURE",
                        s.indicativeScheduleFee() == null || s.indicativeScheduleFee().signum() < 0
                                ? null : s.indicativeScheduleFee(),
                        s.rationale(),
                        s.confidence() == null ? null : Math.max(0.0, Math.min(1.0, s.confidence()))))
                .toList();
    }

    private static void rule(String regex, String item, String description, String type, String fee) {
        RULES.put(Pattern.compile("\\b(" + regex + ")", Pattern.CASE_INSENSITIVE),
                new MbsSuggestion(item, description, type, new BigDecimal(fee),
                        "Keyword match in the clinical notes", 0.6));
    }
}
