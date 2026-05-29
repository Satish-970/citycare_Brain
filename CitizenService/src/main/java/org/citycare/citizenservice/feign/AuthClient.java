package org.citycare.citizenservice.feign;

import org.citycare.citizenservice.dto.request.UserProfileUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AUTHSERVICE")
public interface AuthClient {

    @PutMapping("/api/users/{id}/profile")
    void updateUserProfile(
            @PathVariable Long id,
            @RequestBody UserProfileUpdateRequest request);
}