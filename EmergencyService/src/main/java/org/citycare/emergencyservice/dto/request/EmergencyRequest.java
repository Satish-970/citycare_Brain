package org.citycare.emergencyservice.dto.request;


import org.citycare.emergencyservice.entity.Emergency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmergencyRequest {
    @NotNull(message = "Emergency type (ACCIDENT, HEART_ATTACK, etc.) is required")private Emergency.Type type;
    @NotBlank private String location;
    private String description;
}

