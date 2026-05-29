package org.citycare.patienttreatmentservice.serviceImplementation;

import org.citycare.patienttreatmentservice.dto.AdmitPatientRequest;
import org.citycare.patienttreatmentservice.dto.ApiResponse;
import org.citycare.patienttreatmentservice.dto.TreatmentRequest;
import org.citycare.patienttreatmentservice.entity.Patient;
import org.citycare.patienttreatmentservice.entity.Treatment;
import org.citycare.patienttreatmentservice.exception.BadRequestException;
import org.citycare.patienttreatmentservice.exception.PatientAlreadyAdmittedException;
import org.citycare.patienttreatmentservice.exception.ResourceNotFoundException;
import org.citycare.patienttreatmentservice.feign.CitizenClient;
import org.citycare.patienttreatmentservice.feign.EmergencyClient;
import org.citycare.patienttreatmentservice.feign.UserClient;
import org.citycare.patienttreatmentservice.feign.dto.EmergencyResponse;
import org.citycare.patienttreatmentservice.feign.dto.StaffClient;
import org.citycare.patienttreatmentservice.feign.dto.StaffResponse;
import org.citycare.patienttreatmentservice.repository.PatientRepository;
import org.citycare.patienttreatmentservice.repository.TreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientTreatmentServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock TreatmentRepository treatmentRepository;
    @Mock CitizenClient citizenClient;
    @Mock EmergencyClient emergencyClient;
    @Mock UserClient userClient;
    @Mock StaffClient staffClient;

    @InjectMocks PatientTreatmentService patientTreatmentService;

    private Patient mockPatient;
    private Treatment mockTreatment;

    @BeforeEach
    void setUp() {
        mockPatient = Patient.builder()
                .patientId(1L).citizenId(10L).emergencyId(5L)
                .admissionDate(LocalDate.now()).ward("Ward A")
                .status(Patient.Status.ADMITTED).build();

        mockTreatment = Treatment.builder()
                .treatmentId(1L).patient(mockPatient).assignedById(20L)
                .description("Paracetamol").medicationName("Paracetamol")
                .dosage("500mg").startDate(LocalDate.now())
                .status(Treatment.Status.ONGOING).build();
    }

    // ── Patient Tests ─────────────────────────────────────────────────────────

    @Test
    void admitPatient_duplicateEmergency_throwsPatientAlreadyAdmitted() {
        AdmitPatientRequest req = new AdmitPatientRequest();
        req.setEmergencyId(5L); req.setCitizenId(10L); req.setWard("Ward A");

        when(patientRepository.findByEmergencyId(5L)).thenReturn(Optional.of(mockPatient));

        assertThatThrownBy(() -> patientTreatmentService.admitPatient(req))
                .isInstanceOf(PatientAlreadyAdmittedException.class);
    }

    @Test
    void admitPatient_success() {
        AdmitPatientRequest req = new AdmitPatientRequest();
        req.setEmergencyId(5L); req.setCitizenId(10L); req.setWard("Ward A");

        EmergencyResponse emergencyResponse = new EmergencyResponse();
        emergencyResponse.setEmergencyId(5L); emergencyResponse.setStatus("REPORTED");

        when(patientRepository.findByEmergencyId(5L)).thenReturn(Optional.empty());
        when(emergencyClient.getById(5L)).thenReturn(emergencyResponse);
        doNothing().when(emergencyClient).updateEmergencyStatus(eq(5L), eq("ADMITTED"));
        when(patientRepository.save(any())).thenReturn(mockPatient);

        Patient result = patientTreatmentService.admitPatient(req);

        assertThat(result.getPatientId()).isEqualTo(1L);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void getPatientById_notFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientTreatmentService.getPatientById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPatientById_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));

        Patient result = patientTreatmentService.getPatientById(1L);

        assertThat(result.getPatientId()).isEqualTo(1L);
    }

    @Test
    void getAllPatients_returnsList() {
        when(patientRepository.findAll()).thenReturn(List.of(mockPatient));

        List<Patient> result = patientTreatmentService.getAllPatients();

        assertThat(result).hasSize(1);
    }

    @Test
    void updatePatientStatus_alreadyDischarged_throwsBadRequest() {
        mockPatient.setStatus(Patient.Status.DISCHARGED);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));

        assertThatThrownBy(() -> patientTreatmentService.updatePatientStatus(1L, Patient.Status.STABLE))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updatePatientStatus_toStable_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(patientRepository.save(any())).thenReturn(mockPatient);

        Patient result = patientTreatmentService.updatePatientStatus(1L, Patient.Status.STABLE);

        assertThat(result).isNotNull();
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void updatePatientStatus_toDischarged_notifiesEmergencyService() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(patientRepository.save(any())).thenReturn(mockPatient);

        patientTreatmentService.updatePatientStatus(1L, Patient.Status.DISCHARGED);

        verify(emergencyClient).updateEmergencyStatus(eq(5L), eq("CLOSED"));
    }

    @Test
    void getPatientsByStatus_returnsList() {
        when(patientRepository.findByStatus(Patient.Status.ADMITTED)).thenReturn(List.of(mockPatient));

        List<Patient> result = patientTreatmentService.getPatientsByStatus(Patient.Status.ADMITTED);

        assertThat(result).hasSize(1);
    }

    // ── Treatment Tests ───────────────────────────────────────────────────────

    @Test
    void addTreatment_patientNotFound_throwsResourceNotFoundException() {
        TreatmentRequest req = new TreatmentRequest();
        req.setPatientId(99L);

        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientTreatmentService.addTreatment(20L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addTreatment_patientDischarged_throwsBadRequest() {
        mockPatient.setStatus(Patient.Status.DISCHARGED);
        TreatmentRequest req = new TreatmentRequest();
        req.setPatientId(1L);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));

        assertThatThrownBy(() -> patientTreatmentService.addTreatment(20L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addTreatment_invalidRole_throwsBadRequest() {
        TreatmentRequest req = new TreatmentRequest();
        req.setPatientId(1L); req.setDescription("Test"); req.setMedicationName("Med"); req.setDosage("10mg");

        StaffResponse staffResponse = new StaffResponse();
        staffResponse.setName("Admin User"); staffResponse.setRole("ADMIN");

        ApiResponse<StaffResponse> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(staffResponse);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(staffClient.getStaffById(20L)).thenReturn(apiResponse);

        assertThatThrownBy(() -> patientTreatmentService.addTreatment(20L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DOCTOR or NURSE");
    }

    @Test
    void addTreatment_success() {
        TreatmentRequest req = new TreatmentRequest();
        req.setPatientId(1L); req.setDescription("Paracetamol");
        req.setMedicationName("Paracetamol"); req.setDosage("500mg");

        StaffResponse staffResponse = new StaffResponse();
        staffResponse.setName("Dr. Smith"); staffResponse.setRole("DOCTOR");

        ApiResponse<StaffResponse> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(staffResponse);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
        when(staffClient.getStaffById(20L)).thenReturn(apiResponse);
        when(treatmentRepository.save(any())).thenReturn(mockTreatment);

        Treatment result = patientTreatmentService.addTreatment(20L, req);

        assertThat(result.getTreatmentId()).isEqualTo(1L);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void getTreatmentById_notFound_throwsResourceNotFoundException() {
        when(treatmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientTreatmentService.getTreatmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTreatmentStatus_alreadyFinalized_throwsBadRequest() {
        mockTreatment.setStatus(Treatment.Status.COMPLETED);
        when(treatmentRepository.findById(1L)).thenReturn(Optional.of(mockTreatment));

        assertThatThrownBy(() -> patientTreatmentService.updateTreatmentStatus(1L, Treatment.Status.CANCELLED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateTreatmentStatus_toCompleted_setsEndDate() {
        when(treatmentRepository.findById(1L)).thenReturn(Optional.of(mockTreatment));
        when(treatmentRepository.save(any())).thenReturn(mockTreatment);

        Treatment result = patientTreatmentService.updateTreatmentStatus(1L, Treatment.Status.COMPLETED);

        assertThat(result).isNotNull();
        assertThat(mockTreatment.getEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void getTreatmentsByPatient_patientNotFound_throwsResourceNotFoundException() {
        when(patientRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> patientTreatmentService.getTreatmentsByPatient(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTreatmentsByPatient_success() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(treatmentRepository.findByPatientPatientId(1L)).thenReturn(List.of(mockTreatment));

        List<Treatment> result = patientTreatmentService.getTreatmentsByPatient(1L);

        assertThat(result).hasSize(1);
    }
}
