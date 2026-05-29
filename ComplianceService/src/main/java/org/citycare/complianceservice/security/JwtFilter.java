package org.citycare.complianceservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(header.substring(7)).getPayload();

                if (!claims.getExpiration().before(new Date())) {
                    // Extract userId and role directly from token claims
                    Object userIdClaim = claims.get("userId");
                    Object roleClaim = claims.get("role");

                    Long userId = userIdClaim != null ? Long.valueOf(userIdClaim.toString()) : null;
                    String role = roleClaim != null ? roleClaim.toString() : "COMPLIANCE_OFFICER";

                    // Set as request attributes so controllers can read them
                    if (userId != null) request.setAttribute("userId", userId);
                    request.setAttribute("role", role);

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    claims.getSubject(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                }
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
