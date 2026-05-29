package org.citycare.complianceservice.ServiceImplementation;

import org.citycare.complianceservice.dto.request.AuditRequest;
import org.citycare.complianceservice.dto.request.ComplianceRecordRequest;
import org.citycare.complianceservice.dto.response.AuditLogResponse;
import org.citycare.complianceservice.entity.Audit;
import org.citycare.complianceservice.entity.AuditLog;
import org.citycare.complianceservice.entity.ComplianceRecord;
import org.citycare.complianceservice.exception.ResourceNotFoundException;
import org.citycare.complianceservice.feign.AuthClient;
import org.citycare.complianceservice.feign.EmergencyClient;
import org.citycare.complianceservice.feign.FacilityClient;
import org.citycare.complianceservice.feign.PatientClient;
import org.citycare.complianceservice.repository.AuditLogRepository;
import org.citycare.complianceservice.repository.AuditRepository;
import org.citycare.complianceservice.repository.ComplianceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements org.citycare.complianceservice.service.ComplianceService {

    private final ComplianceRecordRepository recordRepository;
    private final AuditRepository auditRepository;
    private final AuditLogRepository auditLogRepository;

    // OpenFeign clients for cross-service entity validation
    private final FacilityClient facilityClient;
    private final PatientClient patientClient;
    private final EmergencyClient emergencyClient;

    private final AuthClient authClient;

    // ── Compliance Records ────────────────────────────────────────────────────

    @Transactional
    public ComplianceRecord createRecord(Long officerId, ComplianceRecordRequest req) {
        // Validate the target entity exists in the respective service via OpenFeign

        validateEntity(req.getType(), req.getEntityId());

        ComplianceRecord record = ComplianceRecord.builder()
                .entityId(req.getEntityId())
                .type(req.getType())
                .result(req.getResult())
                .date(LocalDate.now())
                .notes(req.getNotes())
                .officerId(officerId)
                .build();
        ComplianceRecord saved = recordRepository.save(record);
        logAction(officerId, "CREATE_COMPLIANCE_RECORD", "compliance_records/" + saved.getComplianceId());

        return saved;
    }

    public List<ComplianceRecord> getAllRecords() {
        return recordRepository.findAll();
    }

    public ComplianceRecord getRecordById(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceRecord", id));
    }

    public List<ComplianceRecord> getRecordsByEntity(Long entityId) {
        return recordRepository.findByEntityId(entityId);
    }

    public List<ComplianceRecord> getRecordsByType(ComplianceRecord.EntityType type) {
        return recordRepository.findByType(type);
    }

    // ── Audits ────────────────────────────────────────────────────────────────

    @Transactional
    public Audit createAudit(Long officerId, AuditRequest req) {
        Audit audit = Audit.builder()
                .officerId(officerId)
                .scope(req.getScope())
                .findings(req.getFindings())
                .date(req.getDate())
                .status(Audit.Status.SCHEDULED)
                .build();
        Audit saved = auditRepository.save(audit);
        logAction(officerId, "CREATE_AUDIT", "audits/" + saved.getAuditId());

        return saved;
    }

    public List<Audit> getAllAudits() {
        return auditRepository.findAll();
    }

    public Audit getAuditById(Long id) {
        return auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit", id));
    }

    @Transactional
    public Audit updateAuditStatus(Long id, Audit.Status status, String findings) {
        Audit audit = getAuditById(id);
        audit.setStatus(status);
        if (findings != null) audit.setFindings(findings);
        return auditRepository.save(audit);
    }

    // ── Audit Logs ────────────────────────────────────────────────────────────

    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<AuditLogResponse> getLogsByUser(Long userId) {
        return auditLogRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        String name = null;
        try { name = authClient.getUserName(log.getUserId()); } catch (Exception ignored) {}
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .userId(log.getUserId())
                .userName(name)
                .action(log.getAction())
                .resource(log.getResource())
                .timestamp(log.getTimestamp())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }



    private void validateEntity(ComplianceRecord.EntityType type, Long entityId) {
        try {
            log.info("Validating {} with ID: {}", type, entityId);

            Object response = switch (type) {
                case FACILITY -> facilityClient.getFacilityById(entityId);
                case PATIENT -> patientClient.getPatientById(entityId);
                case EMERGENCY -> emergencyClient.getEmergencyById(entityId);
            };

            if (response == null) {
                throw new RuntimeException(type + " service is currently unavailable. Please try again later.");
            }

            log.info("Successfully validated {} with ID: {}", type, entityId);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (feign.FeignException.NotFound e) {
            throw new ResourceNotFoundException(type + " with ID " + entityId + " not found in " + type + " service.");
        } catch (feign.FeignException e) {
            log.error("Feign error during {} validation: {} {}", type, e.status(), e.getMessage());
            throw new RuntimeException(type + " service is currently unavailable. Please try again later.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during {} validation: {}", type, e.getMessage());
            throw new RuntimeException(type + " service is currently unavailable. Please try again later.");
        }
    }

    private void logAction(Long userId, String action, String resource) {
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .action(action)
                .resource(resource)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
