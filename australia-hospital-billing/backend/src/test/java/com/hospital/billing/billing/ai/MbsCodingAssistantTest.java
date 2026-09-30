package com.hospital.billing.billing.ai;

import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestion;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionList;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionRequest;
import com.hospital.billing.billing.ai.AiBillingDtos.MbsSuggestionResponse;
import com.hospital.billing.common.ai.LanguageModelClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MbsCodingAssistantTest {

    /** Test double: a model that is switched off. */
    private static final LanguageModelClient DISABLED = new LanguageModelClient() {
        public boolean isEnabled() { return false; }
        public String modelName() { return "rules"; }
        public <T> Optional<T> ask(String system, String user, Class<T> type) { return Optional.empty(); }
    };

    @Test
    void fallsBackToKeywordRulesWhenAiIsDisabled() {
        MbsSuggestionResponse response = new MbsCodingAssistant(DISABLED)
                .suggest(new MbsSuggestionRequest("Chest pain overnight, troponin and ECG ordered, cardiology review", "Cardiology"));

        assertEquals("RULES", response.source());
        List<String> items = response.suggestions().stream().map(MbsSuggestion::mbsItemNo).toList();
        assertTrue(items.contains("66518"), "troponin");
        assertTrue(items.contains("11714"), "ECG");
        assertTrue(items.contains("110"), "cardiology attendance");
    }

    @Test
    void usesAndSanitisesTheModelAnswerWhenAvailable() {
        LanguageModelClient fakeModel = new LanguageModelClient() {
            public boolean isEnabled() { return true; }
            public String modelName() { return "fake-model"; }
            @SuppressWarnings("unchecked")
            public <T> Optional<T> ask(String system, String user, Class<T> type) {
                assertTrue(!user.contains("2123456071"), "Medicare number must be redacted before the model sees it");
                return Optional.of((T) new MbsSuggestionList(List.of(
                        new MbsSuggestion("30445", "Lap cholecystectomy", "surgery", new BigDecimal("899.10"), "notes say lap chole", 1.7),
                        new MbsSuggestion("not-a-number", "junk", "SURGERY", null, null, 0.5))));
            }
        };

        MbsSuggestionResponse response = new MbsCodingAssistant(fakeModel)
                .suggest(new MbsSuggestionRequest("Medicare 2123456071. Laparoscopic cholecystectomy performed.", null));

        assertEquals("AI", response.source());
        assertEquals(1, response.suggestions().size());
        assertEquals("SURGERY", response.suggestions().get(0).itemType());
        assertEquals(1.0, response.suggestions().get(0).confidence());
    }
}
