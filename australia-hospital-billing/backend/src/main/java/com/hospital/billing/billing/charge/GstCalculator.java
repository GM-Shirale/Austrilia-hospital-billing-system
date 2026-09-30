package com.hospital.billing.billing.charge;

import com.hospital.billing.billing.domain.BillItemType;
import com.hospital.billing.common.money.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * GST (ER diagram note 2). Under the GST Act, hospital treatment and medical services are
 * GST-free; taxable supplies are things like non-clinical equipment hire and some
 * pharmacy goods. The medicine catalogue carries its own rate; EQUIPMENT and OTHER lines use
 * the standard rate; everything else is GST-free.
 */
@Component
public class GstCalculator {

    private static final Set<BillItemType> STANDARD_RATED = EnumSet.of(BillItemType.EQUIPMENT, BillItemType.OTHER);

    private final BigDecimal standardRate;

    public GstCalculator(@Value("${app.billing.gst-rate:0.10}") BigDecimal standardRate) {
        this.standardRate = standardRate;
    }

    public BigDecimal rateFor(BillItemType type, BigDecimal explicitRate) {
        if (explicitRate != null) {
            return explicitRate;
        }
        return STANDARD_RATED.contains(type) ? standardRate : BigDecimal.ZERO;
    }

    /** GST on the taxable value (gross - discount). */
    public BigDecimal gst(BigDecimal taxableValue, BigDecimal rate) {
        return Money.percentOf(taxableValue, rate);
    }
}
