package com.hospital.billing.lab.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lab_order_item")
public class LabOrderItem extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Column(nullable = false)
    private Integer quantity;

    /** Price copied from the catalogue when ordered. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String result;

    @Column(name = "resulted_by_name", length = 120)
    private String resultedByName;

    @Column(name = "resulted_at")
    private LocalDateTime resultedAt;

    public void recordResult(String value, String by, LocalDateTime at) {
        this.result = value;
        this.resultedByName = by;
        this.resultedAt = at;
    }
}
