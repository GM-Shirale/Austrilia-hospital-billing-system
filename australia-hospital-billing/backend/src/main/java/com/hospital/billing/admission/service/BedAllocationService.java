package com.hospital.billing.admission.service;

import org.springframework.context.ApplicationEventPublisher;
import com.hospital.billing.common.event.ChargeSourceChangedEvent;
import com.hospital.billing.admission.domain.Admission;
import com.hospital.billing.admission.domain.AdmissionRepository;
import com.hospital.billing.admission.domain.Room;
import com.hospital.billing.admission.domain.RoomAllocation;
import com.hospital.billing.admission.domain.RoomAllocationRepository;
import com.hospital.billing.admission.domain.RoomRepository;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationRequest;
import com.hospital.billing.admission.web.AdmissionDtos.BedAllocationResponse;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bed allocation with three layers of protection against double-booking:
 * <ol>
 *   <li>row lock on the bed ({@code SELECT ... FOR UPDATE});</li>
 *   <li>temporal overlap query (served by ix_allocation_room_period);</li>
 *   <li>PostgreSQL exclusion constraint ex_allocation_no_overlap as the last line of defence.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BedAllocationService implements BedAllocationOperations {

    /** Stand-in for "open ended" when checking overlaps against a stay with no end date. */
    private static final LocalDateTime OPEN_END = LocalDateTime.of(9999, 12, 31, 0, 0);

    private final AdmissionRepository admissionRepository;
    private final RoomRepository roomRepository;
    private final RoomAllocationRepository allocationRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public BedAllocationResponse allocateBed(Long admissionId, BedAllocationRequest request) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new EntityNotFoundException("Admission", admissionId));
        admission.requireActive();
        if (!admission.getCareSetting().requiresBed()) {
            throw new BusinessRuleException("Admission %s is an out-patient (OPD) visit and cannot be given a bed"
                    .formatted(admission.getAdmissionNo()));
        }

        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException("Room", request.roomId()));
        LocalDateTime from = request.fromDate() != null ? request.fromDate() : LocalDateTime.now(clock);
        if (from.isBefore(admission.getAdmissionDate())) {
            throw new BusinessRuleException("Bed allocation cannot start before the admission date");
        }

        // Transfer: close the current stay first so it does not overlap with the new one.
        allocationRepository.findFirstByAdmissionIdAndToDateIsNull(admissionId).ifPresent(current -> {
            if (current.getRoom().getId().equals(room.getId())) {
                throw new BusinessRuleException("Patient is already in " + room.label());
            }
            current.close(from);
            current.getRoom().release();
            allocationRepository.flush();
        });

        if (allocationRepository.countOverlapping(room.getId(), from, OPEN_END) > 0) {
            throw new BusinessRuleException("%s is already allocated for the requested period".formatted(room.label()));
        }
        room.occupy();

        RoomAllocation allocation = new RoomAllocation();
        allocation.setAdmission(admission);
        allocation.setRoom(room);
        allocation.setFromDate(from);
        allocation.setChargePerDay(room.getDailyRate());
        RoomAllocation saved = allocationRepository.save(allocation);
        events.publishEvent(new ChargeSourceChangedEvent(admissionId, "Bed " + room.label()));
        log.info("Allocated {} to admission {}", room.label(), admission.getAdmissionNo());
        return BedAllocationResponse.from(saved);
    }

    @Override
    @Transactional
    public void releaseBed(Long admissionId, LocalDateTime at) {
        allocationRepository.findFirstByAdmissionIdAndToDateIsNull(admissionId).ifPresent(open -> {
            open.close(at);
            open.getRoom().release();
        });
    }

    @Override
    public List<BedAllocationResponse> allocationHistory(Long admissionId) {
        return allocationRepository.findByAdmissionIdOrderByFromDateAsc(admissionId).stream()
                .map(BedAllocationResponse::from)
                .toList();
    }

    @Override
    public List<RoomAllocation> allocationsForBilling(Long admissionId) {
        return allocationRepository.findByAdmissionIdOrderByFromDateAsc(admissionId);
    }
}
