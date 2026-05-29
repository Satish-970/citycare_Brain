package org.citycare.citizenservice.serviceImplementation;

import org.citycare.citizenservice.dto.request.CitizenProfileRequest;
import org.citycare.citizenservice.dto.request.UserProfileUpdateRequest;
import org.citycare.citizenservice.dto.response.CitizenDocumentResponse;
import org.citycare.citizenservice.dto.response.CitizenResponse;
import org.citycare.citizenservice.entity.Citizen;
import org.citycare.citizenservice.entity.CitizenDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.citycare.citizenservice.exception.ResourceNotFoundException;
import org.citycare.citizenservice.feign.AuthClient;
import org.citycare.citizenservice.repository.CitizenDocumentRepository;
import org.citycare.citizenservice.repository.CitizenRepository;
import org.citycare.citizenservice.service.CitizenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenServiceImplementation implements CitizenService {

    private final CitizenRepository citizenRepository;
    private final CitizenDocumentRepository documentRepository;
    private final AuthClient authClient;

    // ── Called by auth-service via OpenFeign on citizen registration ──────────

    /**
     * Auto-creates a minimal citizen profile when a new user registers.
     * Called internally by auth-service via OpenFeign — not by the user directly.
     * If a profile already exists for this userId (e.g. idempotency), it is returned as-is.
     */
    @Transactional
    public Citizen createCitizenFromRegistration(Long userId, String name, String contactInfo) {
        // Idempotent: if profile already exists, return it
        return citizenRepository.findById(userId).orElseGet(() -> {
            log.info("Auto-creating citizen profile for userId={}, name={}", userId, name);
            Citizen citizen = Citizen.builder()
                    .citizenId(userId)
                    .name(name)
                    .contactInfo(contactInfo)
                    .status(Citizen.Status.ACTIVE)
                    .build();
            return citizenRepository.save(citizen);
        });
    }

    // ── Standard CRUD ─────────────────────────────────────────────────────────

    @Transactional
    public Citizen createOrUpdateProfile(Long userId, CitizenProfileRequest req) {


        Citizen citizen = citizenRepository.findById(userId)
                .orElse(new Citizen());

        citizen.setCitizenId(userId);
        citizen.setName(req.getName());
        citizen.setDateOfBirth(req.getDateOfBirth());
        citizen.setGender(req.getGender());
        citizen.setAddress(req.getAddress());
        citizen.setContactInfo(req.getContactInfo());
        citizen.setBloodGroup(req.getBloodGroup());
        citizen.setStatus(Citizen.Status.ACTIVE);


        authClient.updateUserProfile(
                userId,
                new UserProfileUpdateRequest(
                        req.getName(),
                        req.getContactInfo()
                )
        );


        return citizenRepository.save(citizen);
    }

    public Citizen getProfile(Long userId) {
        return citizenRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found for userId: " + userId));
    }

    public Citizen getById(Long citizenId) {
        return citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", citizenId));
    }


    @Override
    public CitizenResponse getCitizenResponseById(Long citizenId) {

        Citizen citizen = citizenRepository.getById(citizenId);

        return CitizenResponse.builder()
                .citizenId(citizen.getCitizenId())
                .name(citizen.getName())
                .contactInfo(citizen.getContactInfo())
                .status("ACTIVE")
                .build();

    }

    public List<Citizen> getAll() {
        return citizenRepository.findAll();
    }

    @Transactional
    public CitizenDocument uploadDocument(Long citizenId, byte[] documentData) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", citizenId));

        CitizenDocument doc = CitizenDocument.builder()
                .citizen(citizen)
                .documentData(documentData)
                .uploadedDate(LocalDate.now())
                .verificationStatus(CitizenDocument.VerificationStatus.PENDING)
                .build();

        return documentRepository.save(doc);
    }

    @Transactional
    public CitizenDocumentResponse verifyDocument(Long documentId,
                                                  CitizenDocument.VerificationStatus status) {

        CitizenDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        doc.setVerificationStatus(status);
        documentRepository.save(doc);
        return toDocumentResponse(doc);
    }

    public List<CitizenDocumentResponse> getDocuments(Long citizenId) {
        return documentRepository.findByCitizenCitizenId(citizenId).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    private CitizenDocumentResponse toDocumentResponse(CitizenDocument doc) {
        return CitizenDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .verificationStatus(doc.getVerificationStatus().name())
                .uploadedDate(doc.getUploadedDate())
                .documentData(doc.getDocumentData() != null
                        ? java.util.Base64.getEncoder().encodeToString(doc.getDocumentData())
                        : null)
                .build();
    }
}
