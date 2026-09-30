package com.hospital.billing.pharmacy.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findTop50ByMedicineIdOrderByPerformedAtDescIdDesc(Long medicineId);
}
