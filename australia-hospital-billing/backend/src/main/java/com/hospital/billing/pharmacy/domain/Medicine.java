package com.hospital.billing.pharmacy.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
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
@Table(name = "medicine")
public class Medicine extends BaseTenantEntity {

    @Column(name = "medicine_name", nullable = false, length = 120)
    private String medicineName;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** 0.1000 = 10% GST; prescription medicines are usually GST-free (0). */
    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_category", nullable = false, length = 20)
    private SupplyCategory supplyCategory = SupplyCategory.PBS;

    @Column(name = "pbs_item_code", length = 10)
    private String pbsItemCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "stock_adjusted_by_name", length = 120)
    private String stockAdjustedByName;

    @Column(name = "stock_adjusted_at")
    private LocalDateTime stockAdjustedAt;

    /** GST rate always follows the supply category (PBS / prescription = 0%, OTC / taxable = 10%). */
    public void classify(SupplyCategory category) {
        this.supplyCategory = category;
        this.gstRate = category.gstRate();
    }

    /** Applies a stock change and stamps who made it; the caller writes the ledger row. */
    public int changeStock(int delta, String by, LocalDateTime at) {
        int newQuantity = stockQuantity + delta;
        if (newQuantity < 0) {
            throw new BusinessRuleException("Insufficient stock of %s: %d available, %d required"
                    .formatted(medicineName, stockQuantity, -delta));
        }
        stockQuantity = newQuantity;
        stockAdjustedByName = by;
        stockAdjustedAt = at;
        return newQuantity;
    }
}
