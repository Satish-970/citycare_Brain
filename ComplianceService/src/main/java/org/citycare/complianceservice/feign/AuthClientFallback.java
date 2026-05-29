package org.citycare.complianceservice.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthClientFallback implements AuthClient {

    @Override
    public String getUserName(Long userId) {
        log.warn("auth-service unavailable – cannot resolve name for userId={}", userId);
        return null;
    }
}
