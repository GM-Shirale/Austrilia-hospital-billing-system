package com.hospital.billing.provider.service;

import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.provider.domain.Doctor;
import com.hospital.billing.provider.domain.DoctorSchedule;
import com.hospital.billing.provider.domain.DoctorScheduleRepository;
import com.hospital.billing.provider.web.ScheduleDtos.ScheduleRequest;
import com.hospital.billing.provider.web.ScheduleDtos.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Weekly doctor rosters: CRUD plus "is the doctor rostered at this time?". */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorScheduleService {

    private final DoctorScheduleRepository scheduleRepository;
    private final ProviderService providerService;

    public List<ScheduleResponse> forDoctor(Long doctorId) {
        return scheduleRepository.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
                .sorted(ORDER)
                .map(ScheduleResponse::from)
                .toList();
    }

    public List<ScheduleResponse> all() {
        return scheduleRepository.findByActiveTrueOrderByDayOfWeekAscStartTimeAsc().stream()
                .sorted(Comparator.comparing((DoctorSchedule s) -> s.getDoctor().getLastName()).thenComparing(ORDER))
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(Long doctorId, ScheduleRequest request) {
        Doctor doctor = providerService.requireActiveDoctor(doctorId);
        validate(doctorId, null, request);
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        apply(schedule, request);
        DoctorSchedule saved = scheduleRepository.save(schedule);
        log.info("Added {} {}-{} to the roster of {}", request.dayOfWeek(), request.startTime(), request.endTime(),
                doctor.displayName());
        return ScheduleResponse.from(saved);
    }

    @Transactional
    public ScheduleResponse update(Long scheduleId, ScheduleRequest request) {
        DoctorSchedule schedule = require(scheduleId);
        validate(schedule.getDoctor().getId(), scheduleId, request);
        apply(schedule, request);
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long scheduleId) {
        scheduleRepository.delete(require(scheduleId));
    }

    /** Doctors without any roster are treated as "on call" (always rostered). */
    public boolean isRostered(Long doctorId, LocalDateTime at) {
        List<DoctorSchedule> blocks = scheduleRepository.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId);
        return blocks.isEmpty() || blocks.stream().anyMatch(block -> block.covers(at));
    }

    private void validate(Long doctorId, Long selfId, ScheduleRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessRuleException("End time must be after the start time");
        }
        boolean clash = scheduleRepository.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
                .filter(s -> !s.getId().equals(selfId))
                .anyMatch(s -> s.overlaps(request.dayOfWeek(), request.startTime(), request.endTime()));
        if (clash) {
            throw new BusinessRuleException("The doctor already has a session overlapping %s %s-%s"
                    .formatted(request.dayOfWeek(), request.startTime(), request.endTime()));
        }
    }

    private void apply(DoctorSchedule schedule, ScheduleRequest request) {
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setSlotMinutes(request.slotMinutes() == null ? 15 : request.slotMinutes());
        schedule.setLocation(request.location() == null ? null : request.location().trim());
    }

    private DoctorSchedule require(Long id) {
        return scheduleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Schedule", id));
    }

    private static final Comparator<DoctorSchedule> ORDER =
            Comparator.comparing(DoctorSchedule::getDayOfWeek).thenComparing(DoctorSchedule::getStartTime);
}
