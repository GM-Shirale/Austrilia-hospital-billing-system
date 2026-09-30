package com.hospital.billing.pharmacy.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findAllByOrderByMedicineNameAsc();

    List<Medicine> findByActiveTrueOrderByMedicineNameAsc();

    boolean existsByMedicineNameIgnoreCase(String medicineName);

    long countByStockQuantityLessThan(int threshold);
}
