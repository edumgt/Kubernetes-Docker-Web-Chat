package org.example.oauth.service;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.oauth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {


    private final JwtUtil jwtUtil;
    private final String successRedirectUri;
    private final boolean secureCookie;
    private final String sameSite;
    private final long tokenValidityMs;

    public CustomAuthenticationSuccessHandler(
            JwtUtil jwtUtil,
            @Value("${app.auth.success-redirect-uri:http://localhost:9999/chat}") String successRedirectUri,
            @Value("${app.security.cookie.secure:false}") boolean secureCookie,
            @Value("${app.security.cookie.same-site:Lax}") String sameSite,
            @Value("${app.security.jwt.expiration-ms:3600000}") long tokenValidityMs
    ) {
        this.jwtUtil = jwtUtil;
        this.successRedirectUri = successRedirectUri;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.tokenValidityMs = tokenValidityMs;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

//        jwt token

        String token = jwtUtil.generateToken(email,name);

        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMillis(tokenValidityMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect(successRedirectUri);
    }
}
