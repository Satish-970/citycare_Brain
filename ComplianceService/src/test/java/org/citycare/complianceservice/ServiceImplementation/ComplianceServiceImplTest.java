package org.citycare.complianceservice.ServiceImplementation;

import org.citycare.complianceservice.dto.request.AuditRequest;
import org.citycare.complianceservice.dto.request.ComplianceRecordRequest;
import org.citycare.complianceservice.entity.Audit;
import org.citycare.complianceservice.entity.AuditLog;
import org.citycare.complianceservice.entity.ComplianceRecord;
import org.citycare.complianceservice.exception.ResourceNotFoundException;
import org.citycare.complianceservice.feign.EmergencyClient;
import org.citycare.complianceservice.feign.FacilityClient;
import org.citycare.complianceservice.feign.PatientClient;
import org.citycare.complianceservice.repository.AuditLogRepository;
import org.citycare.complianceservice.repository.AuditRepository;
import org.citycare.complianceservice.repository.ComplianceRecordRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceImplTest {

    @Mock ComplianceRecordRepository recordRepository;
    @Mock AuditRepository auditRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock FacilityClient facilityClient;
    @Mock PatientClient patientClient;
    @Mock EmergencyClient emergencyClient;

    @InjectMocks ComplianceServiceImpl complianceService;

    private ComplianceRecord mockRecord;
    private Audit mockAudit;

    @BeforeEach
    void setUp() {
        mockRecord = ComplianceRecord.builder()
                .complianceId(1L).entityId(10L)
                .type(ComplianceRecord.EntityType.FACILITY)
                .result(ComplianceRecord.Result.COMPLIANT).officerId(5L)
                .date(LocalDate.now()).build();

        mockAudit = Audit.builder()
                .auditId(1L).officerId(5L)
                .scope("Hospital Wing A").findings("None")
                .date(LocalDate.now()).status(Audit.Status.SCHEDULED).build();
    }

    @Test
    void createRecord_success() {
        ComplianceRecordRequest req = new ComplianceRecordRequest();
        req.setEntityId(10L);
        req.setType(ComplianceRecord.EntityType.FACILITY);
        req.setResult(ComplianceRecord.Result.COMPLIANT);
        req.setDate(LocalDate.now());

        when(recordRepository.save(any())).thenReturn(mockRecord);
        when(auditLogRepository.save(any())).thenReturn(new AuditLog());

        ComplianceRecord result = complianceService.createRecord(5L, req);

        assertThat(result.getComplianceId()).isEqualTo(1L);
        verify(recordRepository).save(any(ComplianceRecord.class));
    }

    @Test
    void getRecordById_notFound_throwsResourceNotFoundException() {
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.getRecordById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRecordById_success() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        ComplianceRecord result = complianceService.getRecordById(1L);

        assertThat(result.getComplianceId()).isEqualTo(1L);
    }

    @Test
    void getAllRecords_returnsList() {
        when(recordRepository.findAll()).thenReturn(List.of(mockRecord));

        List<ComplianceRecord> result = complianceService.getAllRecords();

        assertThat(result).hasSize(1);
    }

    @Test
    void getRecordsByEntity_returnsList() {
        when(recordRepository.findByEntityId(10L)).thenReturn(List.of(mockRecord));

        List<ComplianceRecord> result = complianceService.getRecordsByEntity(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void createAudit_success() {
        AuditRequest req = new AuditRequest();
        req.setScope("Hospital Wing A");
        req.setFindings("None");
        req.setDate(LocalDate.now());

        when(auditRepository.save(any())).thenReturn(mockAudit);
        when(auditLogRepository.save(any())).thenReturn(new AuditLog());

        Audit result = complianceService.createAudit(5L, req);

        assertThat(result.getAuditId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Audit.Status.SCHEDULED);
    }

    @Test
    void getAuditById_notFound_throwsResourceNotFoundException() {
        when(auditRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.getAuditById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAuditStatus_success() {
        when(auditRepository.findById(1L)).thenReturn(Optional.of(mockAudit));
        when(auditRepository.save(any())).thenReturn(mockAudit);

        Audit result = complianceService.updateAuditStatus(1L, Audit.Status.COMPLETED, "All good");

        assertThat(result.getStatus()).isEqualTo(Audit.Status.COMPLETED);
    }

    @Test
    void getAllAudits_returnsList() {
        when(auditRepository.findAll()).thenReturn(List.of(mockAudit));

        List<Audit> result = complianceService.getAllAudits();

        assertThat(result).hasSize(1);
    }

    @Test
    void getLogsByUser_returnsList() {
        AuditLog log = AuditLog.builder().userId(5L).action("CREATE").resource("compliance_records/1").build();
        when(auditLogRepository.findByUserId(5L)).thenReturn(List.of(log));

        List<AuditLog> result = complianceService.getLogsByUser(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(5L);
    }
}
