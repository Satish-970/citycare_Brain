package org.citycare.complianceservice.dto.request;

import org.citycare.complianceservice.entity.ComplianceRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComplianceRecordRequest {

    @NotNull(message = "Entity ID cannot be null")
    @Positive(message = "Entity ID must be a positive number")
    private Long entityId;

    @NotNull(message = "Compliance type (FACILITY, PATIENT, EMERGENCY) is required")
    private ComplianceRecord.EntityType type;

    @NotNull(message = "Compliance result is required")
    private ComplianceRecord.Result result;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}