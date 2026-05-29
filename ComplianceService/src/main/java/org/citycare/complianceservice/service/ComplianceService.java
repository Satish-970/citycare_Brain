package org.citycare.complianceservice.service;

import org.citycare.complianceservice.dto.response.AuditLogResponse;
import org.citycare.complianceservice.dto.request.AuditRequest;
import org.citycare.complianceservice.dto.request.ComplianceRecordRequest;
import org.citycare.complianceservice.entity.Audit;
import org.citycare.complianceservice.entity.AuditLog;
import org.citycare.complianceservice.entity.ComplianceRecord;

import java.util.List;

public interface ComplianceService {

    ComplianceRecord createRecord(Long officerId, ComplianceRecordRequest req);

    List<ComplianceRecord> getAllRecords();

    ComplianceRecord getRecordById(Long id);

    List<ComplianceRecord> getRecordsByEntity(Long entityId);

    List<ComplianceRecord> getRecordsByType(ComplianceRecord.EntityType type);

    Audit createAudit(Long officerId, AuditRequest req);

    List<Audit> getAllAudits();

    Audit getAuditById(Long id);

    Audit updateAuditStatus(Long id, Audit.Status status, String findings);

    List<AuditLogResponse> getAllLogs();

    List<AuditLogResponse> getLogsByUser(Long userId);
}