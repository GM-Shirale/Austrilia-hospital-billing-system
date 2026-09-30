package com.hospital.billing.provider.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByDepartmentNameIgnoreCase(String departmentName);

    List<Department> findAllByOrderByDepartmentNameAsc();
}
