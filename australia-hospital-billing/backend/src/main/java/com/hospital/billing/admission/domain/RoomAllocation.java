package com.hospital.billing.admission.domain;

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
import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "room_allocation")
public class RoomAllocation extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "from_date", nullable = false)
    private LocalDateTime fromDate;

    @Column(name = "to_date")
    private LocalDateTime toDate;

    /** Rate is copied at allocation time so later price changes don't rewrite history. */
    @Column(name = "charge_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargePerDay;

    public boolean isOpen() {
        return toDate == null;
    }

    public void close(LocalDateTime at) {
        this.toDate = at.isAfter(fromDate) ? at : fromDate.plusMinutes(1);
    }

    /** Hospital days: any part of a 24h period is charged as a day, minimum one day. */
    public long billableDays(LocalDateTime now) {
        LocalDateTime end = toDate != null ? toDate : now;
        long minutes = Math.max(0, Duration.between(fromDate, end).toMinutes());
        long days = (minutes + (24 * 60) - 1) / (24 * 60);
        return Math.max(1, days);
    }
}
