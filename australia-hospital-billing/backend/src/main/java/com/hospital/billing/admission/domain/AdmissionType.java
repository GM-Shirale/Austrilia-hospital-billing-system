package com.hospital.billing.admission.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admission_type")
public class AdmissionType extends BaseTenantEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "type_name", nullable = false, length = 80)
    private String typeName;
}
