package com.hospital.billing.common.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiRedactorTest {

    @Test
    void removesMedicareNumbersPhonesEmailsAndDates() {
        String redacted = PiiRedactor.redact(
                "Pt Medicare 2123 45607 1, DOB 12/04/1985, ph 0412 345 678, email emily@example.com. Chest pain.");

        assertFalse(redacted.contains("45607"));
        assertFalse(redacted.contains("0412"));
        assertFalse(redacted.contains("emily@"));
        assertFalse(redacted.contains("1985"));
        assertTrue(redacted.contains("[MEDICARE]"));
        assertTrue(redacted.contains("Chest pain."));
    }

    @Test
    void keepsClinicalTextUntouched() {
        assertEquals("Troponin elevated, ECG shows ST changes", PiiRedactor.redact("Troponin elevated, ECG shows ST changes"));
        assertEquals("", PiiRedactor.redact(null));
    }
}
