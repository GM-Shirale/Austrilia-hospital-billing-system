package com.hospital.billing.provider.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    @EntityGraph(attributePaths = {"doctor", "doctor.department"})
    List<DoctorSchedule> findByActiveTrueOrderByDayOfWeekAscStartTimeAsc();
}
