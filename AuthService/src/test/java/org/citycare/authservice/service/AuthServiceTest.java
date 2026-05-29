package org.citycare.authservice.service;

import org.citycare.authservice.dto.CreateStaffRequest;
import org.citycare.authservice.dto.RegisterRequest;
import org.citycare.authservice.entity.User;
import org.citycare.authservice.exception.EmailAlreadyRegisteredException;
import org.citycare.authservice.exception.ResourceNotFoundException;
import org.citycare.authservice.feign.CitizenClient;
import org.citycare.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CitizenClient citizenClient;

    @InjectMocks AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L).name("John").email("john@example.com")
                .password("encoded").phone("1234567890")
                .role(User.Role.CITIZEN).status(User.Status.ACTIVE).build();
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setName("John"); req.setEmail("john@example.com");
        req.setPassword("pass"); req.setPhone("1234567890");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(mockUser);

        var response = authService.register(req);

        assertThat(response.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsEmailAlreadyRegistered() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john@example.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("john@example.com");
    }

    @Test
    void createStaff_throwsEmailAlreadyRegistered() {
        CreateStaffRequest req = new CreateStaffRequest();
        req.setEmail("staff@example.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.createStaff(req))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        User result = authService.getUserById(1L);

        assertThat(result.getUserId()).isEqualTo(1L);
    }

    @Test
    void deactivateUser_setsStatusInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        User result = authService.deactivateUser(1L);

        assertThat(result.getStatus()).isEqualTo(User.Status.INACTIVE);
    }

    @Test
    void activateUser_setsStatusActive() {
        mockUser.setStatus(User.Status.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        User result = authService.activateUser(1L);

        assertThat(result.getStatus()).isEqualTo(User.Status.ACTIVE);
    }

    @Test
    void getAllStaff_returnsDoctorsAndNurses() {
        when(userRepository.findByRoleIn(List.of(User.Role.DOCTOR, User.Role.NURSE)))
                .thenReturn(List.of(mockUser));

        List<User> staff = authService.getAllStaff();

        assertThat(staff).hasSize(1);
    }
}
