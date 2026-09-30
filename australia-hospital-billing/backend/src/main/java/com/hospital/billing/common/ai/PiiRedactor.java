package com.hospital.billing.common.ai;

import java.util.regex.Pattern;

/**
 * Removes direct identifiers from free text before it leaves the hospital network
 * (Australian Privacy Act / APP 8: cross-border disclosure). Prompts built by the
 * application never contain patient names or Medicare numbers; this protects the free-text
 * fields that staff type in.
 */
public final class PiiRedactor {

    private static final Pattern MEDICARE = Pattern.compile("\\b[2-6]\\d{3}\\s?\\d{5}\\s?\\d(?:\\s?\\d)?\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("(\\+61|\\b0)[2-478](\\s?\\d){8}\\b");
    private static final Pattern DATE = Pattern.compile("\\b\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}\\b");

    private PiiRedactor() {
    }

    public static String redact(String text) {
        if (text == null) {
            return "";
        }
        String result = MEDICARE.matcher(text).replaceAll("[MEDICARE]");
        result = EMAIL.matcher(result).replaceAll("[EMAIL]");
        result = PHONE.matcher(result).replaceAll("[PHONE]");
        return DATE.matcher(result).replaceAll("[DATE]");
    }
}
