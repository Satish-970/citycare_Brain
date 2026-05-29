package org.citycare.emergencyservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    public Claims getClaims(String token) {
        // 1. Create the Key
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));

        // 2. Use parserBuilder() - This is the standard for 0.11.x
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build() // This should now compile perfectly
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}