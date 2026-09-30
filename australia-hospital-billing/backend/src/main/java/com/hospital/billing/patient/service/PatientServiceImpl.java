package com.hospital.billing.patient.service;

import com.hospital.billing.common.domain.DocumentNumberGenerator;
import com.hospital.billing.common.domain.DocumentType;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.common.web.PageResponse;
import com.hospital.billing.patient.domain.Patient;
import com.hospital.billing.patient.domain.PatientContact;
import com.hospital.billing.patient.domain.PatientRepository;
import com.hospital.billing.patient.web.PatientDtos.ContactRequest;
import com.hospital.billing.patient.web.PatientDtos.PatientRequest;
import com.hospital.billing.patient.web.PatientDtos.PatientResponse;
import com.hospital.billing.patient.web.PatientDtos.PatientSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final DocumentNumberGenerator documentNumberGenerator;

    @Override
    public PageResponse<PatientSummary> search(String term, Pageable pageable) {
        Page<Patient> page = StringUtils.hasText(term)
                ? patientRepository.search(term.trim(), pageable)
                : patientRepository.findAll(pageable);
        return PageResponse.from(page.map(PatientSummary::from));
    }

    @Override
    public PatientResponse get(Long id) {
        return PatientResponse.from(requirePatient(id));
    }

    @Override
    @Transactional
    public PatientResponse register(PatientRequest request) {
        checkBusinessRules(request);
        String medicareNo = normaliseMedicare(request.medicareNo());
        if (medicareNo != null && patientRepository.existsByMedicareNoAndMedicareIrn(medicareNo, request.medicareIrn())) {
            throw new DuplicateResourceException("A patient with this Medicare number and IRN is already registered");
        }
        Patient patient = new Patient();
        patient.setMrn(documentNumberGenerator.next(DocumentType.PATIENT));
        apply(patient, request);
        if (request.contacts() != null) {
            request.contacts().forEach(c -> patient.addContact(toContact(c)));
        }
        Patient saved = patientRepository.save(patient);
        log.info("Registered patient {} ({})", saved.getId(), saved.getMrn());
        return PatientResponse.from(saved);
    }

    @Override
    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = requirePatient(id);
        checkBusinessRules(request);
        String medicareNo = normaliseMedicare(request.medicareNo());
        boolean medicareChanged = !Objects.equals(medicareNo, patient.getMedicareNo())
                || !Objects.equals(request.medicareIrn(), patient.getMedicareIrn());
        if (medicareNo != null && medicareChanged
                && patientRepository.existsByMedicareNoAndMedicareIrn(medicareNo, request.medicareIrn())) {
            throw new DuplicateResourceException("A patient with this Medicare number and IRN is already registered");
        }
        apply(patient, request);
        return PatientResponse.from(patient);
    }

    @Override
    @Transactional
    public PatientResponse addContact(Long patientId, ContactRequest request) {
        Patient patient = requirePatient(patientId);
        patient.addContact(toContact(request));
        patientRepository.flush();
        return PatientResponse.from(patient);
    }

    @Override
    @Transactional
    public PatientResponse removeContact(Long patientId, Long contactId) {
        Patient patient = requirePatient(patientId);
        PatientContact contact = patient.getContacts().stream()
                .filter(c -> c.getId().equals(contactId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Contact", contactId));
        patient.removeContact(contact);
        return PatientResponse.from(patient);
    }

    @Override
    public Patient requirePatient(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Patient", id));
    }

    /** Rules that span fields (single-field rules are Bean Validation annotations on PatientRequest). */
    private static void checkBusinessRules(PatientRequest request) {
        if (request.dob() != null && request.dob().isBefore(LocalDate.now().minusYears(130))) {
            throw new BusinessRuleException("Date of birth is more than 130 years ago; please check it");
        }
        boolean hasMedicare = StringUtils.hasText(request.medicareNo());
        if (hasMedicare && request.medicareIrn() == null) {
            throw new BusinessRuleException("Medicare IRN (1-9, the number next to the patient's name on the card) is required with a Medicare number");
        }
        if (!hasMedicare && request.medicareIrn() != null) {
            throw new BusinessRuleException("Medicare IRN was given without a Medicare card number");
        }
    }

    private void apply(Patient patient, PatientRequest request) {
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setDob(request.dob());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        patient.setSuburb(request.suburb());
        patient.setState(request.state());
        patient.setPostcode(request.postcode());
        patient.setMedicareNo(normaliseMedicare(request.medicareNo()));
        patient.setMedicareIrn(request.medicareIrn());
        if (request.status() != null) {
            patient.setStatus(request.status());
        }
    }

    private PatientContact toContact(ContactRequest request) {
        PatientContact contact = new PatientContact();
        contact.setName(request.name().trim());
        contact.setRelation(request.relation().trim());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setPrimaryContact(request.primaryContact());
        return contact;
    }

    private static String normaliseMedicare(String value) {
        return StringUtils.hasText(value) ? value.replace(" ", "") : null;
    }
}
