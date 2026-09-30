package com.hospital.billing.admission.service;

import com.hospital.billing.admission.web.AdmissionDtos.AdmissionResponse;
import com.hospital.billing.admission.web.AdmissionDtos.AdmitRequest;
import com.hospital.billing.admission.web.AdmissionDtos.ChangeDoctorRequest;
import com.hospital.billing.admission.web.AdmissionDtos.DischargeRequest;

/** Interface Segregation: state-changing admission operations (admit -> discharge / cancel). */
public interface AdmissionLifecycleOperations {

    AdmissionResponse admit(AdmitRequest request);

    AdmissionResponse discharge(Long admissionId, DischargeRequest request);

    AdmissionResponse cancel(Long admissionId);

    /** Attach / replace the attending doctor of an ACTIVE admission. */
    AdmissionResponse changeAttendingDoctor(Long admissionId, ChangeDoctorRequest request);
}
