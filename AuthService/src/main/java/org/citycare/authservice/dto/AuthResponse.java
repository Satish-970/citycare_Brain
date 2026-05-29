package org.citycare.authservice.dto;

import org.citycare.authservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.citycare.authservice.entity.User.Status;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private User.Role role;
    private String token;
    private Status status;
}
