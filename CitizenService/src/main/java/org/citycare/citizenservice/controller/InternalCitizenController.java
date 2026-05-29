package org.citycare.citizenservice.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.citycare.citizenservice.dto.request.CitizenInternalCreateRequest;
import org.citycare.citizenservice.dto.response.ApiResponse;
import org.citycare.citizenservice.entity.Citizen;
import org.citycare.citizenservice.service.CitizenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints called by other microservices via OpenFeign.
 * These are NOT exposed to external clients (protected by API gateway routing rules).
 */
@RestController
@RequestMapping("/citizens/internal")
@RequiredArgsConstructor
public class InternalCitizenController {

    private final CitizenService citizenService;

    /**
     * Called by auth-service via OpenFeign immediately after a new CITIZEN registers.
     * Auto-creates a basic citizen profile so the citizen row exists from day one.
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Citizen>> createCitizenFromRegistration(
            @Valid @RequestBody CitizenInternalCreateRequest request) {
        Citizen citizen = citizenService.createCitizenFromRegistration(
                request.getUserId(), request.getName(), request.getContactInfo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Citizen profile auto-created", citizen));
    }

    /**
     * Called by emergency-service / compliance-service via OpenFeign to check if a citizen exists.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Citizen>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Citizen",
                citizenService.getProfile(userId)));
    }


}
