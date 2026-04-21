package com.andrei.demo.util;

import com.andrei.demo.model.Person;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    private Date getCurrentDate() {
        return Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(Person person) {
        return Jwts.builder()
                .subject(person.getEmail())
                .issuer("demo-spring-boot-backend")
                .issuedAt(getCurrentDate())
                .claims(Map.of(
                        "userId", person.getId(),
                        "role", person.getRole().name()
                ))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getRoleFromToken(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    public boolean checkClaims(String token) {
        Claims claims = getAllClaimsFromToken(token);

        if (!"demo-spring-boot-backend".equals(claims.getIssuer())) {
            log.error("Invalid token issuer");
            return false;
        }
        if (claims.getExpiration().before(getCurrentDate())) {
            log.error("Token has expired");
            return false;
        }
        if (claims.getIssuedAt() == null || claims.getIssuedAt().after(getCurrentDate())) {
            log.error("Token issued-at date is invalid");
            return false;
        }
        if (claims.get("userId") == null || claims.get("role") == null) {
            log.error("Token is missing required claims");
            return false;
        }

        log.info("Token is valid. User ID: {}, Role: {}", claims.get("userId"), claims.get("role"));
        return true;
    }
}