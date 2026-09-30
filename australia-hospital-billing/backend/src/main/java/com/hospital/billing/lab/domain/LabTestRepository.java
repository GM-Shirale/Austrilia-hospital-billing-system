package com.hospital.billing.lab.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    List<LabTest> findAllByOrderByCategoryAscTestNameAsc();

    boolean existsByTestCodeIgnoreCase(String testCode);

    List<LabTest> findByActiveTrueOrderByCategoryAscTestNameAsc();
}
