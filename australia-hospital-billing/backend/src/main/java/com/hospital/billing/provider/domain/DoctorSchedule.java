package com.hospital.billing.provider.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One weekly roster block of a doctor, e.g. MONDAY 08:00-17:00 in the Cardiology Clinic. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctor_schedule")
public class DoctorSchedule extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes = 15;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private boolean active = true;

    public boolean overlaps(DayOfWeek day, LocalTime start, LocalTime end) {
        return active && dayOfWeek == day && start.isBefore(endTime) && end.isAfter(startTime);
    }

    /** True when the moment falls inside this roster block. */
    public boolean covers(LocalDateTime at) {
        LocalTime time = at.toLocalTime();
        return active && at.getDayOfWeek() == dayOfWeek && !time.isBefore(startTime) && time.isBefore(endTime);
    }
}
