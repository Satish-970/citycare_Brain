package org.citycare.patienttreatmentservice.repository;


import org.citycare.patienttreatmentservice.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmergencyId(Long emergencyId);
    List<Patient> findByCitizenId(Long citizenId);
    List<Patient> findByStatus(Patient.Status status);
}