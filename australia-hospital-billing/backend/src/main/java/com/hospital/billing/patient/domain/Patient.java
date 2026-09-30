package com.hospital.billing.patient.domain;

import com.hospital.billing.common.domain.BaseTenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient")
public class Patient extends BaseTenantEntity {

    @Column(nullable = false, length = 20, updatable = false)
    private String mrn;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 200)
    private String address;

    @Column(length = 80)
    private String suburb;

    @Column(length = 10)
    private String state;

    @Column(length = 4)
    private String postcode;

    @Column(nullable = false, length = 60)
    private String country = "Australia";

    @Column(name = "medicare_no", length = 10)
    private String medicareNo;

    /** Individual Reference Number: the patient's position on the Medicare card (1-9). */
    @Column(name = "medicare_irn")
    private Short medicareIrn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatientStatus status = PatientStatus.ACTIVE;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("primaryContact DESC, id ASC")
    private List<PatientContact> contacts = new ArrayList<>();

    public String fullName() {
        return firstName + " " + lastName;
    }

    public int ageOn(LocalDate date) {
        return Period.between(dob, date).getYears();
    }

    public boolean hasMedicare() {
        return medicareNo != null && !medicareNo.isBlank();
    }

    public void addContact(PatientContact contact) {
        if (contact.isPrimaryContact()) {
            contacts.forEach(existing -> existing.setPrimaryContact(false));
        }
        contact.setPatient(this);
        contacts.add(contact);
    }

    public void removeContact(PatientContact contact) {
        contacts.remove(contact);
        contact.setPatient(null);
    }
}
