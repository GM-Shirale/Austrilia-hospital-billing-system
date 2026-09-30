package com.hospital.billing.lab.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Pathology / imaging catalogue entry, with its MBS item for Medicare rebates. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lab_test")
public class LabTest extends BaseTenantEntity {

    @Column(name = "test_code", nullable = false, length = 20)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 120)
    private String testName;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "mbs_item_no", length = 10)
    private String mbsItemNo;

    @Column(name = "mbs_schedule_fee", precision = 12, scale = 2)
    private BigDecimal mbsScheduleFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 20)
    private LabTestType testType = LabTestType.PATHOLOGY;

    /** Pathology / imaging services are GST-free medical services; false = 10% GST. */
    @Column(name = "gst_free", nullable = false)
    private boolean gstFree = true;

    @Column(nullable = false)
    private boolean active = true;

    public BigDecimal gstRate() {
        return gstFree ? BigDecimal.ZERO : STANDARD_GST;
    }

    private static final BigDecimal STANDARD_GST = new BigDecimal("0.10");
}
