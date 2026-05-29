package org.citycare.complianceservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AuditRequest {

    @NotBlank(message = "Audit scope is required")
    @Size(min = 3, max = 100, message = "Scope must be between 3 and 100 characters")
    private String scope;

    @NotNull(message = "Audit date is required")
    @FutureOrPresent(message = "Audit date cannot be in the past")
    private LocalDate date;

    @Size(max = 1000, message = "Findings cannot exceed 1000 characters")
    private String findings;
}