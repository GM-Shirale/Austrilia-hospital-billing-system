package com.hospital.billing.patient.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** tenant_id is appended by Hibernate; uses ix_patient_tenant_name / ix_patient_tenant_medicare. */
    @Query("""
            select p from Patient p
            where lower(p.firstName) like lower(concat('%', :term, '%'))
               or lower(p.lastName)  like lower(concat('%', :term, '%'))
               or p.mrn        like concat('%', :term, '%')
               or p.medicareNo like concat('%', :term, '%')
               or p.phone      like concat('%', :term, '%')
            """)
    Page<Patient> search(@Param("term") String term, Pageable pageable);

    boolean existsByMedicareNoAndMedicareIrn(String medicareNo, Short medicareIrn);

    long countByStatus(PatientStatus status);
}
