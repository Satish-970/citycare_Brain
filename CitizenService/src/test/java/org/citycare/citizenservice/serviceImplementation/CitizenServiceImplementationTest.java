package org.citycare.citizenservice.serviceImplementation;

import org.citycare.citizenservice.dto.request.CitizenProfileRequest;
import org.citycare.citizenservice.dto.request.UserProfileUpdateRequest;
import org.citycare.citizenservice.dto.response.CitizenDocumentResponse;
import org.citycare.citizenservice.entity.Citizen;
import org.citycare.citizenservice.entity.CitizenDocument;
import org.citycare.citizenservice.exception.ResourceNotFoundException;
import org.citycare.citizenservice.feign.AuthClient;
import org.citycare.citizenservice.repository.CitizenDocumentRepository;
import org.citycare.citizenservice.repository.CitizenRepository;
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
class CitizenServiceImplementationTest {

    @Mock CitizenRepository citizenRepository;
    @Mock CitizenDocumentRepository documentRepository;
    @Mock AuthClient authClient;

    @InjectMocks CitizenServiceImplementation citizenService;

    private Citizen mockCitizen;

    @BeforeEach
    void setUp() {
        mockCitizen = Citizen.builder()
                .citizenId(1L).name("John").contactInfo("9876543210")
                .status(Citizen.Status.ACTIVE).build();
    }

    @Test
    void createCitizenFromRegistration_newCitizen_savesAndReturns() {
        when(citizenRepository.findById(1L)).thenReturn(Optional.empty());
        when(citizenRepository.save(any())).thenReturn(mockCitizen);

        Citizen result = citizenService.createCitizenFromRegistration(1L, "John", "9876543210");

        assertThat(result.getName()).isEqualTo("John");
        verify(citizenRepository).save(any(Citizen.class));
    }

    @Test
    void createCitizenFromRegistration_existingCitizen_returnsExisting() {
        when(citizenRepository.findById(1L)).thenReturn(Optional.of(mockCitizen));

        Citizen result = citizenService.createCitizenFromRegistration(1L, "John", "9876543210");

        assertThat(result.getCitizenId()).isEqualTo(1L);
        verify(citizenRepository, never()).save(any());
    }

    @Test
    void createOrUpdateProfile_success() {
        CitizenProfileRequest req = new CitizenProfileRequest();
        req.setName("John"); req.setContactInfo("9876543210");
        req.setGender(Citizen.Gender.MALE); req.setAddress("Bangalore");
        req.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(mockCitizen));
        when(citizenRepository.save(any())).thenReturn(mockCitizen);
        doNothing().when(authClient).updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class));

        Citizen result = citizenService.createOrUpdateProfile(1L, req);

        assertThat(result).isNotNull();
        verify(citizenRepository).save(any(Citizen.class));
        verify(authClient).updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class));
    }

    @Test
    void getProfile_notFound_throwsResourceNotFoundException() {
        when(citizenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProfile_success() {
        when(citizenRepository.findById(1L)).thenReturn(Optional.of(mockCitizen));

        Citizen result = citizenService.getProfile(1L);

        assertThat(result.getCitizenId()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(citizenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsList() {
        when(citizenRepository.findAll()).thenReturn(List.of(mockCitizen));

        List<Citizen> result = citizenService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void uploadDocument_citizenNotFound_throwsResourceNotFoundException() {
        when(citizenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenService.uploadDocument(99L, new byte[]{1, 2, 3}))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploadDocument_success() {
        CitizenDocument doc = CitizenDocument.builder()
                .documentId(1L).citizen(mockCitizen)
                .documentData(new byte[]{1, 2, 3})
                .uploadedDate(LocalDate.now())
                .verificationStatus(CitizenDocument.VerificationStatus.PENDING).build();

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(mockCitizen));
        when(documentRepository.save(any())).thenReturn(doc);

        CitizenDocument result = citizenService.uploadDocument(1L, new byte[]{1, 2, 3});

        assertThat(result.getVerificationStatus()).isEqualTo(CitizenDocument.VerificationStatus.PENDING);
    }

    @Test
    void verifyDocument_notFound_throwsResourceNotFoundException() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenService.verifyDocument(99L, CitizenDocument.VerificationStatus.VERIFIED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyDocument_success() {
        CitizenDocument doc = CitizenDocument.builder()
                .documentId(1L).citizen(mockCitizen)
                .uploadedDate(LocalDate.now())
                .verificationStatus(CitizenDocument.VerificationStatus.PENDING).build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        CitizenDocumentResponse result = citizenService.verifyDocument(1L, CitizenDocument.VerificationStatus.VERIFIED);

        assertThat(result.getVerificationStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void getDocuments_returnsMappedList() {
        CitizenDocument doc = CitizenDocument.builder()
                .documentId(1L).citizen(mockCitizen)
                .uploadedDate(LocalDate.now())
                .verificationStatus(CitizenDocument.VerificationStatus.PENDING).build();

        when(documentRepository.findByCitizenCitizenId(1L)).thenReturn(List.of(doc));

        List<CitizenDocumentResponse> result = citizenService.getDocuments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVerificationStatus()).isEqualTo("PENDING");
    }
}
