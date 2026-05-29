package org.citycare.citizenservice.service;

import org.citycare.citizenservice.dto.request.CitizenProfileRequest;
import org.citycare.citizenservice.dto.response.CitizenDocumentResponse;
import org.citycare.citizenservice.dto.response.CitizenResponse;
import org.citycare.citizenservice.entity.Citizen;
import org.citycare.citizenservice.entity.CitizenDocument;

import java.util.List;

public interface CitizenService {
    Citizen createCitizenFromRegistration(Long userId, String name, String contactInfo);
    Citizen createOrUpdateProfile(Long userId, CitizenProfileRequest req);Citizen getProfile(Long userId);
    Citizen getById(Long citizenId);
    List<Citizen> getAll();
    CitizenDocument uploadDocument(Long citizenId, byte[] documentData);
    CitizenDocumentResponse verifyDocument(Long documentId, CitizenDocument.VerificationStatus status);
    List<CitizenDocumentResponse> getDocuments(Long citizenId);
    CitizenResponse getCitizenResponseById(Long citizenId);
}
