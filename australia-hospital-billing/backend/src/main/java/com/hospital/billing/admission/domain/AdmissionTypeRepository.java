package com.hospital.billing.admission.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdmissionTypeRepository extends JpaRepository<AdmissionType, Long> {

    List<AdmissionType> findAllByOrderByTypeNameAsc();
}
