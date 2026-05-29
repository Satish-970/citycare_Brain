package org.citycare.patienttreatmentservice.serviceImplementation;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.citycare.patienttreatmentservice.dto.AdmitPatientRequest;
import org.citycare.patienttreatmentservice.dto.ApiResponse;
import org.citycare.patienttreatmentservice.dto.TreatmentRequest;
import org.citycare.patienttreatmentservice.entity.Patient;
import org.citycare.patienttreatmentservice.entity.Treatment;
import org.citycare.patienttreatmentservice.exception.BadRequestException;
import org.citycare.patienttreatmentservice.exception.PatientAlreadyAdmittedException;
import org.citycare.patienttreatmentservice.exception.ResourceNotFoundException;
import org.citycare.patienttreatmentservice.exception.UnauthorizedException;
import org.citycare.patienttreatmentservice.feign.CitizenClient;
import org.citycare.patienttreatmentservice.feign.EmergencyClient;
import org.citycare.patienttreatmentservice.feign.UserClient;
import org.citycare.patienttreatmentservice.feign.dto.*;
import org.citycare.patienttreatmentservice.repository.PatientRepository;
import org.citycare.patienttreatmentservice.repository.TreatmentRepository;
import org.citycare.patienttreatmentservice.service.PatientTreatmentInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientTreatmentService implements PatientTreatmentInterface {

    private final PatientRepository patientRepository;
    private final TreatmentRepository treatmentRepository;

    // OpenFeign clients for cross-service communication
    private final CitizenClient citizenClient;
    private final EmergencyClient emergencyClient;
    private final UserClient userClient;
    private final StaffClient staffClient;
    // --------------------------------------------Patient -----------------------------------------

    @Transactional
    public Patient admitPatient(AdmitPatientRequest req) {
        log.info("Starting admission process for Emergency ID: {}", req.getEmergencyId());

        // 1. DUPLICATE CHECK (Validation Step)
        // Idhi mundhe check cheyali, lekunda citizenClient/emergencyClient call avvadam waste.
        patientRepository.findByEmergencyId(req.getEmergencyId()).ifPresent(p -> {
            log.error("Duplicate admission attempt. Emergency ID {} already has Patient ID {}",
                    req.getEmergencyId(), p.getPatientId());

            // Meeru adigina custom message ikkada undi
            throw new PatientAlreadyAdmittedException(
                    "Can't admit patient because he is already in the hospital. Patient ID: " + p.getPatientId());
        });

        // 2. Validate Citizen
        try {
            CitizenResponse citizen = citizenClient.getById(req.getCitizenId());
            log.info("Validated citizen: {} (id={})", citizen.getName(), citizen.getCitizenId());
        } catch (Exception e) {
            log.warn("Could not validate citizen {} from citizen-service: {}", req.getCitizenId(), e.getMessage());
        }

        // 3. Update Emergency Status
        try {
            EmergencyResponse emergency = emergencyClient.getById(req.getEmergencyId());
            log.info("Validated emergency id={} with status={}", emergency.getEmergencyId(), emergency.getStatus());

            // Status update chese mundhu check cheyadam better
            emergencyClient.updateEmergencyStatus(req.getEmergencyId(), "ADMITTED");
            log.info("Updated emergency {} status to ADMITTED", req.getEmergencyId());
        } catch (Exception e) {
            log.error("Failed to update emergency status: {}", e.getMessage());
            throw new RuntimeException("Emergency status update failed."); // Re-throw so @Transactional rolls back
        }

        // 4. Create and Save Patient Record
        Patient patient = Patient.builder()
                .citizenId(req.getCitizenId())
                .emergencyId(req.getEmergencyId())
                .admissionDate(LocalDate.now())
                .ward(req.getWard())
                .notes(req.getNotes())
                .status(Patient.Status.ADMITTED)
                .build();

        Patient savedPatient = patientRepository.save(patient);

        // 5. Release Ambulance
        try {
            emergencyClient.releaseAmbulance(req.getEmergencyId());
            log.info("Ambulance released for Emergency ID: {}", req.getEmergencyId());
        } catch (Exception e) {
            log.error("Ambulance release failed: {}", e.getMessage());
            // Dependency issues unte ikkada custom logic handle cheyali
        }

        log.info("Patient successfully admitted with ID: {}", savedPatient.getPatientId());
        return savedPatient;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    @Transactional
    public Patient updatePatientStatus(Long id, Patient.Status status) {
        log.info("Updating status for Patient ID: {} to {}", id, status);

        // 1. Fetch Patient and Check Existence
        // getPatientById(id) lo already orElseThrow(ResourceNotFoundException) undi ani anukuntunna
        Patient patient = getPatientById(id);

        // 2. Business Logic: Discharged ayina patient ni malli update cheyanivvakudadu
        if (patient.getStatus() == Patient.Status.DISCHARGED) {
            throw new BadRequestException("Patient already discharged! Cannot update status anymore.");
        }

        // 3. Update Local Patient Status
        patient.setStatus(status);
        if (status == Patient.Status.DISCHARGED) {
            patient.setDischargeDate(LocalDate.now());
        }

        // 4. External Service Call (Emergency Service)
        if (status == Patient.Status.DISCHARGED) {
            try {
                log.info("Notifying Emergency Service to CLOSE Emergency ID: {}", patient.getEmergencyId());
                emergencyClient.updateEmergencyStatus(patient.getEmergencyId(), "CLOSED");
                log.info("Emergency {} successfully CLOSED via Feign", patient.getEmergencyId());
            } catch (Exception e) {
                // Ikkada log error chestunnam kani process aapatledu (Soft Fail)
                // Endukante Patient discharge avvadam main priority
                log.error("Failed to close emergency {} in emergency-service: {}",
                        patient.getEmergencyId(), e.getMessage());

                // Optional: System critical ayithe ikkada kuda throw RuntimeException kottochu
                // throw new RuntimeException("Could not sync with Emergency Service");
            }
        }

        // 5. Save Changes to Local DB
        try {
            Patient updatedPatient = patientRepository.save(patient);
        log.info("Patient ID: {} status updated to {} successfully", id, status);

        return updatedPatient;
        } catch (Exception e) {
            log.error("Database error while updating patient status: {}", e.getMessage());
            throw new RuntimeException("Failed to save patient status update");
        }
    }

    public EmergencyResponse getEmergencyForPatient(Long patientId) {
        Patient patient = getPatientById(patientId);
        return emergencyClient.getById(patient.getEmergencyId());
    }



    // --------------------------------------------  Treatment -------------------------------------------------------

    public List<Patient> getPatientsByStatus(Patient.Status status) {
        log.info("Fetching patients with status: {}", status);
        return patientRepository.findByStatus(status);
    }

    @Transactional
    public Treatment addTreatment( TreatmentRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        Long assignedById = Long.parseLong(auth.getName());
        System.out.println("assignedById: " + assignedById);
        log.info("Attempting to add treatment for Patient ID: {} by Staff ID: {}", req.getPatientId(), assignedById);

        // 1. Fetch Patient and Validate Status
        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", req.getPatientId()));

        if (patient.getStatus() == Patient.Status.DISCHARGED) {
            log.warn("Treatment assignment failed: Patient {} is already discharged", req.getPatientId());
            throw new BadRequestException("Cannot assign treatment. Patient is already DISCHARGED.");
        }

        // 2. Fetch Staff via Feign Client with Precise Error Handling
        StaffResponse staff;
        try {
            ApiResponse<StaffResponse> response = staffClient.getStaffById(assignedById);

            // Service response ichi, andulo data lekapothe (Internal logic failure)
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Staff", assignedById);
            }

            staff = response.getData();
            log.info("Validated Staff: {} | Role: {}", staff.getName(), staff.getRole());

        } catch (feign.FeignException e) {
            // IKKADA LOGIC: Feign 404 (Not Found) thitithe kachitanga Staff ledhane ardham
            log.error("Feign Error Status: {} for Staff ID: {}", e.status(), assignedById);

            if (e.status() == 404) {
                throw new ResourceNotFoundException("Staff", assignedById);
            }

            // Okavela 403 Forbidden vasthe
            if (e.status() == 403) {
                throw new BadRequestException("Access Denied: Security blocked the call to Facility Service.");
            }

            // Vere emi service error vachina (500, etc)
            throw new RuntimeException("Facility Service error: " + e.status());

        } catch (Exception e) {
            // Asalu connection-ey lekapothe (Service down) idi trigger avthundi
            log.error("General Exception during staff validation: {}", e.getMessage());
            throw new RuntimeException("Could not validate staff. Facility Service might be down.");
        }

        // 3. Staff Role Validation
        String role = staff.getRole();
        if (role == null || (!role.equalsIgnoreCase("DOCTOR") && !role.equalsIgnoreCase("NURSE"))) {
            log.warn("Unauthorized attempt by ID: {} with role: {}", assignedById, role);
            throw new BadRequestException("Unauthorized: Only DOCTOR or NURSE can assign treatments. Current role: " + role);
        }

        // 4. Create and Save Treatment
        Treatment treatment = Treatment.builder()
                .patient(patient)
                .assignedById(assignedById)
                .description(req.getDescription())
                .medicationName(req.getMedicationName())
                .dosage(req.getDosage())
                .startDate(LocalDate.now())
                .status(Treatment.Status.ONGOING)
                .build();

        Treatment savedTreatment = treatmentRepository.save(treatment);
        log.info("Treatment (ID: {}) successfully assigned by {}", savedTreatment.getTreatmentId(), staff.getName());

        return savedTreatment;
    }
    public List<Treatment> getTreatmentsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        return treatmentRepository.findByPatientPatientId(patientId);
    }

    public Treatment getTreatmentById(Long id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", id));
    }
//    @Transactional
//    public Treatment updateTreatmentStatus(Long id, Treatment.Status status) {
//        log.info("Updating status for Treatment ID: {} to {}", id, status);
//
//        // 1. Fetch Treatment and Validate Existence
//        // findById(id) lo 'id' ni vethukuthunnam
//        Treatment treatment = treatmentRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Treatment", id));
//
//        // 2. Business Logic: Status Updates
//        try {
//            // Kotha status ni set chestunnam
//            treatment.setStatus(status);
//
//            // Oka vela treatment COMPLETED leda CANCELLED ayithe, end date ivvala set cheyali
//            if (status == Treatment.Status.COMPLETED || status == Treatment.Status.CANCELLED) {
//                treatment.setEndDate(LocalDate.now());
//                log.info("Treatment ID: {} marked as {}, setting end date to today.", id, status);
//            }
//
//            // 3. Save to Database
//            Treatment updatedTreatment = treatmentRepository.save(treatment);
//            log.info("Treatment ID: {} status updated successfully", id);
//
//            return updatedTreatment;
//
//        } catch (Exception e) {
//            log.error("Error occurred while updating treatment status for ID {}: {}", id, e.getMessage());
//            // Database issue vaste generic RuntimeException throw chestunnam
//            throw new RuntimeException("Could not update treatment status due to a server error.");
//        }
//    }

    @Transactional
    public Treatment updateTreatmentStatus(Long id, Treatment.Status newStatus) {
        log.info("Updating status for Treatment ID: {} to {}", id, newStatus);

        // 1. Fetch Treatment and Validate Existence
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", id));

        // 2. Business Logic: Finalized Status Check
        // Treatment already COMPLETED leda CANCELLED ayithe, inka marchanivvakudadu
        if (treatment.getStatus() == Treatment.Status.COMPLETED ||
                treatment.getStatus() == Treatment.Status.CANCELLED) {

            log.warn("Update failed: Treatment ID {} is already in final state: {}", id, treatment.getStatus());
            throw new BadRequestException("Treatment is already finalized (" + treatment.getStatus() + ") and cannot be updated.");
        }

        try {
            // 3. Update Status
            treatment.setStatus(newStatus);

            // 4. Handle End Date
            if (newStatus == Treatment.Status.COMPLETED || newStatus == Treatment.Status.CANCELLED) {
                treatment.setEndDate(LocalDate.now());
                log.info("Treatment ID: {} marked as {}, setting end date to today.", id, newStatus);
            }

            // 5. Save to Database
            Treatment updatedTreatment = treatmentRepository.save(treatment);
            log.info("Treatment ID: {} status updated successfully to {}", id, newStatus);

            return updatedTreatment;

        } catch (Exception e) {
            log.error("Error occurred while updating treatment status for ID {}: {}", id, e.getMessage());
            // Generic RuntimeException to ensure transaction rollback
            throw new RuntimeException("Could not update treatment status due to a server error.");
        }
    }
    @Transactional(readOnly = true)
    public List<Treatment> getAllTreatments() {
        return treatmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TreatmentSummaryResponse> getMyAssigned(Long staffId) {
        log.info("Fetching all treatments assigned by Staff ID: {}", staffId);

        // 1. Staff check (Feign Call badulu simple ga validate cheyali)
        // Note: Database query lo records rakapothe staff lenatte or treatments lenatte.
        // Oka vela strict ga staff existence check cheyalante userClient call cheyali.
        try {
            ResponseEntity<ApiResponse<UserResponse>> staffResponse = userClient.getUserById(staffId);
            if (staffResponse.getBody() == null || staffResponse.getBody().getData() == null) {
                throw new ResourceNotFoundException("Staff", staffId);
            }
        } catch (Exception e) {
            log.warn("Could not verify staff {} from AuthService, proceeding to fetch treatments anyway.", staffId);
        }

        try {
            // 2. Database nunchi treatments list tevali
            List<Treatment> treatments = treatmentRepository.findByAssignedById(staffId);

            // 3. Result list create cheyali
            List<TreatmentSummaryResponse> responseList = new ArrayList<>();

            // 4. For-each loop logic
            for (Treatment t : treatments) {
                TreatmentSummaryResponse dto = TreatmentSummaryResponse.builder()
                        .treatmentId(t.getTreatmentId())
                        .patientId(t.getPatient().getPatientId())
                        .description(t.getDescription())
                        .medicationName(t.getMedicationName())
                        .dosage(t.getDosage())
                        .status(t.getStatus().toString())
                        .startDate(t.getStartDate())
                        .endDate(t.getEndDate())
                        .build();

                responseList.add(dto);
            }

            log.info("Successfully fetched {} treatments for staff ID: {}", responseList.size(), staffId);
            return responseList;

        } catch (Exception e) {
            log.error("Error in getMyAssigned for staff {}: {}", staffId, e.getMessage());
            throw new RuntimeException("Server error while fetching assigned treatments.");
        }
    }

    @Transactional(readOnly = true)
    public List<TreatmentSummaryResponse> getTreatmentsByDoctorId(Long doctorId) {
        log.info("Fetching treatments from DB for assignedById: {}", doctorId);

        // 1. Database nundi direct ga list techi
        List<Treatment> treatments = treatmentRepository.findByAssignedById(doctorId);

        if (treatments.isEmpty()) {
            log.warn("No treatments found for Doctor ID: {}", doctorId);
        }

        // 2. DTO loki mapping (Extra API calls lekunda)
        List<TreatmentSummaryResponse> responseList = new ArrayList<>();
        for (Treatment t : treatments) {
            responseList.add(TreatmentSummaryResponse.builder()
                    .treatmentId(t.getTreatmentId())
                    .patientId(t.getPatient().getPatientId())
                    .description(t.getDescription())
                    .medicationName(t.getMedicationName())
                    .dosage(t.getDosage())
                    .status(t.getStatus().toString())
                    .startDate(t.getStartDate())
                    .endDate(t.getEndDate())
                    // Doctor Name manam fetch cheyatledhu kabatti ID ne pampochu leda empty pettocchu

                    .build());
        }

        return responseList;
    }

    // ── Cross-service enrichment ──────────────────────────────────────────────

    /**
     * Fetches citizen details from citizen-service for a given patient.
     * Used to enrich patient data with citizen info via OpenFeign.
     */
//    public CitizenResponse getCitizenForPatient(Long patientId) {
//        Patient patient = getPatientById(patientId);
//        return citizenClient.getCitizenById(patient.getCitizenId());
//    }

    /**
     * Fetches emergency details from emergency-service for a given patient.
     */

}
