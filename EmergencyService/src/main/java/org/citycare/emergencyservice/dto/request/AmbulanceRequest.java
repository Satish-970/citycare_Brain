package org.citycare.emergencyservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AmbulanceRequest {
    @NotBlank(message = "Vehicle number cannot be empty")
    @Pattern(regexp = "^[A-Z0-9-]{5,15}$", message = "Invalid vehicle number format")
    private String vehicleNumber;
    private String model;
}
