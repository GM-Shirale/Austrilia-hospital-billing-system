package com.hospital.billing.admission.service;

import com.hospital.billing.admission.domain.RoomAllocation;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationRequest;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationResponse;

import java.time.LocalDateTime;
import java.util.List;

/** Interface Segregation: bed management only. */
public interface BedAllocationOperations {

    /** Allocates a bed; if the patient already has one, this is a transfer (old stay is closed). */
    BedAllocationResponse allocateBed(Long admissionId, BedAllocationRequest request);

    /** Closes the open stay (on discharge/cancel) and frees the bed. */
    void releaseBed(Long admissionId, LocalDateTime at);

    List<BedAllocationResponse> allocationHistory(Long admissionId);

    /** Entities for the billing module's room-charge collector. */
    List<RoomAllocation> allocationsForBilling(Long admissionId);
}
