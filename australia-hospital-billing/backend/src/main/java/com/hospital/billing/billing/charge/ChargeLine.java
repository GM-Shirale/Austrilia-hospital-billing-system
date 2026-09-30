package com.hospital.billing.billing.charge;

import com.hospital.billing.billing.domain.BillItemType;

import java.math.BigDecimal;

/**
 * A billable event collected from a clinical module, before GST and adjudication.
 *
 * @param mbsScheduleFee per-unit MBS schedule fee (null when the item is not an MBS service)
 * @param gstRate        explicit GST rate (e.g. from the medicine), or null to use the default rule
 */
public record ChargeLine(BillItemType itemType,
                         Long referenceId,
                         String description,
                         String mbsItemNo,
                         BigDecimal mbsScheduleFee,
                         BigDecimal quantity,
                         BigDecimal unitPrice,
                         BigDecimal gstRate) {
}
