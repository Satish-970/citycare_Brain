package org.citycare.patienttreatmentservice.feign;

import lombok.Data;
import org.citycare.patienttreatmentservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;



import org.citycare.patienttreatmentservice.feign.dto.UserResponse;
 // Nee common package nunchi
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AuthService" ,fallback = UserClientFallback.class) // Fallback unte add cheyyi
public interface UserClient {

    @GetMapping("/admin/users/{id}")
    ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") Long id);
}
