package com.hospital.billing.adjudication;

import java.math.BigDecimal;

public record LineBenefit(Long billItemId, BigDecimal amount, String note) {
}
