package org.citycare.authservice.controller;

import org.citycare.authservice.dto.*;
import org.citycare.authservice.entity.User;
import org.citycare.authservice.repository.UserRepository;
import org.citycare.authservice.service.AuthService;
import org.citycare.authservice.service.JWTService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login and Admin user management")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @PostMapping("/auth/register")
    @Operation(summary = "Register new citizen account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = authentication.isAuthenticated() ? jwtService.generateToken(user) : null;

        AuthResponse response = AuthResponse.builder()
                .userId(user.getUserId()).name(user.getName())
                .email(user.getEmail()).role(user.getRole()).status(user.getStatus()).token(token).build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    // ── Admin: Staff ──────────────────────────────────────────────────────────

    @PostMapping("/admin/staff")
    @Operation(summary = "[ADMIN] Create Doctor or Nurse account")
    public ResponseEntity<ApiResponse<User>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Staff created", authService.createStaff(request)));
    }

    @GetMapping("/admin/staff")
    @Operation(summary = "[ADMIN] List all Doctors and Nurses")
    public ResponseEntity<ApiResponse<List<User>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.ok("All staff", authService.getAllStaff()));
    }

    @PostMapping("/admin/dispatchers")
    @Operation(summary = "[ADMIN] Create Dispatcher account")
    public ResponseEntity<ApiResponse<User>> createDispatcher(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Dispatcher created", authService.createDispatcher(request)));
    }

    @GetMapping("/admin/dispatchers")
    @Operation(summary = "[ADMIN] List all Dispatchers")
    public ResponseEntity<ApiResponse<List<User>>> getAllDispatchers() {
        return ResponseEntity.ok(ApiResponse.ok("All dispatchers", authService.getAllDispatchers()));
    }

    @PostMapping("/admin/compliance-officers")
    @Operation(summary = "[ADMIN] Create Compliance Officer account")
    public ResponseEntity<ApiResponse<User>> createComplianceOfficer(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Compliance officer created", authService.createComplianceOfficer(request)));
    }



    // ── Admin: Users ──────────────────────────────────────────────────────────

    @GetMapping("/admin/users")
    @Operation(summary = "[ADMIN] List all users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("All users", authService.getAllUsers()));
    }

    @GetMapping("/admin/users/{id}")
    @Operation(summary = "[ADMIN] Get user by ID")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User", authService.getUserById(id)));
    }

    @PatchMapping("/admin/users/{id}/deactivate")
    @Operation(summary = "[ADMIN] Deactivate user account")
    public ResponseEntity<ApiResponse<User>> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", authService.deactivateUser(id)));
    }

    @PatchMapping("/admin/users/{id}/activate")
    @Operation(summary = "[ADMIN] Activate user account")
    public ResponseEntity<ApiResponse<User>> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User activated", authService.activateUser(id)));
    }


    @GetMapping("/internal/users/{id}/name")
    public ResponseEntity<String> getUserName(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id).getName());
    }

    @PutMapping("/users/{id}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'CITIZEN')")
    public ResponseEntity<Void> updateUserProfile(
            @PathVariable Long id,
            @RequestBody @Valid UserProfileUpdateRequest request) {

        authService.updateUserProfile(id, request);
        return ResponseEntity.ok().build();
    }

}
