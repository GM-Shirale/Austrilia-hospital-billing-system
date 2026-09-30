package com.hospital.billing.provider.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /** Fetch the department in the same query (avoids N+1 when listing doctors). */
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Doctor> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "department")
    Page<Doctor> findByDepartmentId(Long departmentId, Pageable pageable);

    boolean existsByProviderNoIgnoreCase(String providerNo);

    long countByActiveTrue();
}
