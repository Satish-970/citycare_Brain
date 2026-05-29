package org.citycare.complianceservice.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String auth = attrs.getRequest().getHeader("Authorization");
                if (auth != null && !auth.isBlank()) template.header("Authorization", auth);

                String userId = attrs.getRequest().getHeader("X-Auth-UserId");
                if (userId != null && !userId.isBlank()) template.header("X-Auth-UserId", userId);

                String role = attrs.getRequest().getHeader("X-User-Role");
                if (role != null && !role.isBlank()) template.header("X-User-Role", role);
            }
        };
    }
}
