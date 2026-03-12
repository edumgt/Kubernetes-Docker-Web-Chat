package org.example.chatservice.service.admin;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Service
public class AdminAuthService {

    private final String adminEmail;
    private final String adminPassword;
    private final Key signingKey;
    private final long tokenValidityMs;

    public AdminAuthService(
            @Value("${app.admin.auth.email:admin@test.com}") String adminEmail,
            @Value("${app.admin.auth.password:123456}") String adminPassword,
            @Value("${app.admin.auth.token-secret:chat-admin-secret-change-this-token-key-123456789}") String tokenSecret,
            @Value("${app.admin.auth.token-expiration-ms:28800000}") long tokenValidityMs
    ) {
        if (!StringUtils.hasText(tokenSecret) || tokenSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Admin token secret must be at least 32 bytes");
        }
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.signingKey = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityMs = tokenValidityMs;
    }

    public boolean authenticate(String email, String password) {
        return adminEmail.equals(email) && adminPassword.equals(password);
    }

    public String issueToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenValidityMs);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", "chat-admin")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<String> getEmailIfValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String role = claims.get("role", String.class);
            if (!"chat-admin".equals(role)) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long getTokenValidityMs() {
        return tokenValidityMs;
    }
}
