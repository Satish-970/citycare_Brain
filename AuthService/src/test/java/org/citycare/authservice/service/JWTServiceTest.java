//package org.citycare.authservice.service;
//
//import io.jsonwebtoken.Claims;
//import org.citycare.authservice.entity.User;
//import org.citycare.authservice.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class JWTServiceTest {
//
//    @Mock UserRepository userRepository;
//
//    @InjectMocks JWTService jwtService;
//
//    private static final String SECRET = "dGVzdHNlY3JldGtleWZvcmp3dHRlc3RpbmdwdXJwb3Nl";
//    private User mockUser;
//
//    @BeforeEach
//    void setUp() {
//        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
//        ReflectionTestUtils.setField(jwtService, "expirationMs", 1800000L);
//
//        mockUser = User.builder()
//                .userId(1L).name("John").email("john@example.com")
//                .role(User.Role.CITIZEN).build();
//    }
//
//    @Test
//    void generateToken_returnsNonNullToken() {
//        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
//
//        String token = jwtService.generateToken("john@example.com");
//
//        assertThat(token).isNotBlank();
//    }
//
//    @Test
//    void extractUserName_returnsCorrectSubject() {
//        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
//        String token = jwtService.generateToken("john@example.com");
//
//        String username = jwtService.extractUserName(token);
//
//        assertThat(username).isEqualTo("john@example.com");
//    }
//
//    @Test
//    void validateToken_validToken_returnsTrue() {
//        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
//        String token = jwtService.generateToken("john@example.com");
//
//        UserDetails userDetails = org.springframework.security.core.userdetails.User
//                .withUsername("john@example.com").password("pass").roles("CITIZEN").build();
//
//        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
//    }
//
//    @Test
//    void extractClaim_returnsSubject() {
//        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
//        String token = jwtService.generateToken("john@example.com");
//
//        String subject = jwtService.extractClaim(token, Claims::getSubject);
//
//        assertThat(subject).isEqualTo("john@example.com");
//    }
//}
