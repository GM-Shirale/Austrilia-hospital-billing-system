package com.hospital.billing.billing.domain;

import java.util.EnumSet;
import java.util.Set;

/** ITEM_TYPE values from the ER diagram. */
public enum BillItemType {
    ROOM,
    DOCTOR,
    CONSULTATION,
    SURGERY,
    PROCEDURE,
    LAB,
    PHARMACY,
    NURSING,
    EQUIPMENT,
    OTHER;

    private static final Set<BillItemType> MEDICAL_SERVICES = EnumSet.of(DOCTOR, CONSULTATION, SURGERY, PROCEDURE, LAB);
    private static final Set<BillItemType> HOSPITAL_CHARGES = EnumSet.of(ROOM, NURSING, EQUIPMENT);

    /** Professional services billed against an MBS item (Medicare 75% + fund 25%). */
    public boolean isMedicalService() {
        return MEDICAL_SERVICES.contains(this);
    }

    /** Accommodation / theatre / nursing / prostheses: covered by the fund per cover tier. */
    public boolean isHospitalCharge() {
        return HOSPITAL_CHARGES.contains(this);
    }
}
