package com.hospital.billing.admission.service;

import com.hospital.billing.admission.domain.Room;
import com.hospital.billing.admission.domain.RoomRepository;
import com.hospital.billing.admission.domain.RoomStatus;
import com.hospital.billing.admission.web.AdmissionDtos.RoomRequest;
import com.hospital.billing.admission.web.AdmissionDtos.RoomResponse;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final com.hospital.billing.admission.domain.RoomAllocationRepository allocationRepository;

    public List<RoomResponse> list(RoomStatus status) {
        List<Room> rooms = status == null
                ? roomRepository.findAllByOrderByRoomNoAscBedNoAsc()
                : roomRepository.findByStatusOrderByRoomNoAscBedNoAsc(status);
        return rooms.stream().map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        if (roomRepository.existsByRoomNoIgnoreCaseAndBedNoIgnoreCase(request.roomNo(), request.bedNo())) {
            throw new DuplicateResourceException("Room %s bed %s already exists".formatted(request.roomNo(), request.bedNo()));
        }
        Room room = new Room();
        room.setRoomNo(request.roomNo().trim().toUpperCase());
        room.setBedNo(request.bedNo().trim().toUpperCase());
        room.setRoomType(request.roomType());
        room.setDailyRate(Money.of(request.dailyRate()));
        room.setStatus(request.status() == RoomStatus.UNDER_MAINTENANCE ? RoomStatus.UNDER_MAINTENANCE : RoomStatus.AVAILABLE);
        return RoomResponse.from(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room", id));
        room.setRoomType(request.roomType());
        room.setDailyRate(Money.of(request.dailyRate()));
        if (request.status() != null && request.status() != room.getStatus()) {
            if (room.getStatus() == RoomStatus.OCCUPIED || request.status() == RoomStatus.OCCUPIED) {
                throw new BusinessRuleException("Occupancy is managed through bed allocation, not edited directly");
            }
            room.setStatus(request.status());
        }
        return RoomResponse.from(room);
    }

    /** Beds with history are kept for audit (set them UNDER_MAINTENANCE instead). */
    @Transactional
    public void delete(Long id) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room", id));
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new BusinessRuleException("%s is occupied and cannot be deleted".formatted(room.label()));
        }
        if (allocationRepository.existsByRoomId(id)) {
            throw new BusinessRuleException("%s has admission history; mark it UNDER_MAINTENANCE instead of deleting"
                    .formatted(room.label()));
        }
        roomRepository.delete(room);
    }
}
