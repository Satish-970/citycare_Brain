package org.citycare.authservice.service;

import org.citycare.authservice.dto.AuthResponse;
import org.citycare.authservice.dto.CreateStaffRequest;
import org.citycare.authservice.dto.RegisterRequest;
import org.citycare.authservice.dto.UserProfileUpdateRequest;
import org.citycare.authservice.entity.User;
import org.citycare.authservice.exception.EmailAlreadyRegisteredException;
import org.citycare.authservice.exception.ResourceNotFoundException;
import org.citycare.authservice.feign.CitizenClient;
import org.citycare.authservice.feign.dto.CitizenCreateRequest;
import org.citycare.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // OpenFeign: auto-create citizen profile after citizen registration
    private final CitizenClient citizenClient;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }
        return doRegister(request);
    }

    @Transactional
    protected AuthResponse doRegister(RegisterRequest request) {

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.CITIZEN)
                .status(User.Status.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);

        // Auto-create citizen profile in citizen-service via OpenFeign
        try {
            CitizenCreateRequest citizenReq = CitizenCreateRequest.builder()
                    .userId(savedUser.getUserId())
                    .name(savedUser.getName())
                    .contactInfo(savedUser.getPhone())
                    .build();
            var citizenResp = citizenClient.createCitizenProfile(citizenReq);
            if (citizenResp == null) {
                log.error("Citizen profile creation failed for userId={} — CitizenService may be down or returned null", savedUser.getUserId());
                throw new RuntimeException("Registration failed: could not create citizen profile. Please try again.");
            }
            log.info("Auto-created citizen profile (citizenId={}) for userId={}",
                    citizenResp.getCitizenId(), savedUser.getUserId());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not auto-create citizen profile for userId={}: {}", savedUser.getUserId(), e.getMessage());
            throw new RuntimeException("Registration failed: could not create citizen profile. Please try again.");
        }



        return mapToResponse(savedUser);
    }

    public User createStaff(CreateStaffRequest req) {
        validateUniqueEmail(req.getEmail());
        return saveUser(req, req.getRole());
    }

    public User createDispatcher(CreateStaffRequest req) {
        validateUniqueEmail(req.getEmail());
        return saveUser(req, User.Role.DISPATCHER);
    }

    public User createComplianceOfficer(CreateStaffRequest req) {
        validateUniqueEmail(req.getEmail());
        return saveUser(req, User.Role.COMPLIANCE_OFFICER);
    }



    @Transactional
    protected User saveUser(CreateStaffRequest req, User.Role role) {
        User saved = userRepository.save(User.builder()
                .name(req.getName()).email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone()).role(role)
                .status(User.Status.ACTIVE).build());

        return saved;
    }

    public List<User> getAllStaff() {
        return userRepository.findByRoleIn(List.of(User.Role.DOCTOR, User.Role.NURSE));
    }

    public List<User> getAllDoctors() {
        return userRepository.findByRoleIn(List.of(User.Role.DOCTOR));
    }

    public List<User> getAllNurses() {
        return userRepository.findByRoleIn(List.of(User.Role.NURSE));
    }

    public List<User> getAllDispatchers() {
        return userRepository.findByRoleIn(List.of(User.Role.DISPATCHER));
    }

    public List<User> getAllComplianceOfficers() {
        return userRepository.findByRoleIn(List.of(User.Role.COMPLIANCE_OFFICER));
    }



    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public User deactivateUser(Long id) {
        User user = getUserById(id);
        user.setStatus(User.Status.INACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public User activateUser(Long id) {
        User user = getUserById(id);
        user.setStatus(User.Status.ACTIVE);
        return userRepository.save(user);
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }



    private AuthResponse mapToResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getUserId()).name(user.getName())
                .email(user.getEmail()).role(user.getRole()).build();
    }


    @Transactional
    public void updateUserProfile(Long userId, UserProfileUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setName(request.getName());
        user.setPhone(request.getContactInfo());

        // No need to call save() explicitly
        // JPA will update the entity on transaction commit
    }

}
