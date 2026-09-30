package com.hospital.billing.admission.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import com.hospital.billing.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One bed (room number + bed number). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "room")
public class Room extends BaseTenantEntity {

    @Column(name = "room_no", nullable = false, length = 20)
    private String roomNo;

    @Column(name = "bed_no", nullable = false, length = 10)
    private String bedNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    public String label() {
        return "Room " + roomNo + " / Bed " + bedNo;
    }

    public void occupy() {
        if (status != RoomStatus.AVAILABLE) {
            throw new BusinessRuleException("%s is %s and cannot be allocated".formatted(label(), status));
        }
        status = RoomStatus.OCCUPIED;
    }

    public void release() {
        if (status == RoomStatus.OCCUPIED) {
            status = RoomStatus.AVAILABLE;
        }
    }
}
