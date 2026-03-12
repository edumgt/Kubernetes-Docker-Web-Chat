package org.example.chatservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key signingKey;
    private final long tokenValidity;

    public JwtUtil(
            @Value("${app.security.jwt.secret}") String secretKey,
            @Value("${app.security.jwt.expiration-ms:3600000}") long tokenValidity
    ) {
        if (secretKey == null || secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.tokenValidity = tokenValidity;
    }

    public String generateToken(String email, String name, String provider) {
        return Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .claim("provider", provider)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + tokenValidity))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract all claims from a token.
    public Claims extractAllClaims(String token) {
        Jws<Claims> jwsClaims = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
        return jwsClaims.getBody();
    }

    // Validate that the token is valid (i.e., the subject matches and it hasn't expired)
    public boolean isTokenValid(String token, String email) {
        final String username = extractAllClaims(token).getSubject();
        return (username.equals(email) && !isTokenExpired(token));
    }

    // Check if the token has expired.
    private boolean isTokenExpired(String token) {
        final Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }



}
