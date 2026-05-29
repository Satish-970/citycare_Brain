package org.citycare.complianceservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AuthService", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping(value = "/api/internal/users/{id}/name", produces = "text/plain")
    String getUserName(@PathVariable("id") Long userId);
}
