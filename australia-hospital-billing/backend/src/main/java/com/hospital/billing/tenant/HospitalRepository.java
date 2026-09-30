package com.hospital.billing.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    List<Hospital> findByActiveTrueOrderByNameAsc();
}
