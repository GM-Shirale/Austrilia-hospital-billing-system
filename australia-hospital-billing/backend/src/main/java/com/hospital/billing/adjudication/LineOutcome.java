package com.hospital.billing.adjudication;

import java.math.BigDecimal;

/** Final split of one bill line: medicare + privateFund + patientGap = net amount. */
public record LineOutcome(Long billItemId, BigDecimal medicare, BigDecimal privateFund, BigDecimal patientGap) {
}
