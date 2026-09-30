package com.hospital.billing.admission.domain;

import com.hospital.billing.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionTest {

    private static final LocalDateTime ADMITTED = LocalDateTime.of(2026, 9, 21, 10, 0);

    @Test
    @DisplayName("Length of stay: every started 24 hours is a day, same-day stay counts as 1")
    void lengthOfStayMatchesAccommodationBilling() {
        Admission admission = admission();
        assertEquals(1, admission.lengthOfStayDays(ADMITTED.plusHours(3)));
        assertEquals(3, admission.lengthOfStayDays(ADMITTED.plusDays(3)));
        assertEquals(4, admission.lengthOfStayDays(ADMITTED.plusDays(3).plusMinutes(1)));
    }

    @Test
    @DisplayName("Discharge fixes the stay and blocks further clinical changes")
    void dischargeFreezesTheStay() {
        Admission admission = admission();
        admission.discharge(ADMITTED.plusDays(2));

        assertEquals(2, admission.lengthOfStayDays(ADMITTED.plusDays(30)));
        assertFalse(admission.isActive());
        assertThrows(BusinessRuleException.class, admission::requireActive);
        assertThrows(BusinessRuleException.class, () -> admission.changeAttendingDoctor(null));
    }

    @Test
    void onlyInpatientsNeedABed() {
        assertTrue(CareSetting.IPD.requiresBed());
        assertFalse(CareSetting.OPD.requiresBed());
    }

    private static Admission admission() {
        Admission admission = new Admission();
        admission.setAdmissionNo("ADM-TEST");
        admission.setAdmissionDate(ADMITTED);
        return admission;
    }
}
