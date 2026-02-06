package org.example.profileservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "yourSuperSecretKeyYourSuperSecretKey";
    private final long TOKEN_VALIDITY = 3600000; // 1 hour

    public String generateToken(String email, String name, String provider) {
        return Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .claim("provider", provider)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract all claims from a token.
    public Claims extractAllClaims(String token) {
        Jws<Claims> jwsClaims = Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
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
