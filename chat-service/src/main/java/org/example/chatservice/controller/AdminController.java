package org.example.chatservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.chatservice.service.admin.AdminAuthService;
import org.example.chatservice.service.admin.AdminMonitoringService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/chat/admin")
public class AdminController {

    private static final String ADMIN_COOKIE_NAME = "CHAT_ADMIN_TOKEN";

    private final AdminAuthService adminAuthService;
    private final AdminMonitoringService adminMonitoringService;

    public AdminController(AdminAuthService adminAuthService, AdminMonitoringService adminMonitoringService) {
        this.adminAuthService = adminAuthService;
        this.adminMonitoringService = adminMonitoringService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AdminLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and password are required");
        }

        if (!adminAuthService.authenticate(request.email(), request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        String token = adminAuthService.issueToken(request.email());
        ResponseCookie cookie = ResponseCookie.from(ADMIN_COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(adminAuthService.getTokenValidityMs()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "authenticated", true,
                        "email", request.email()
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expiredCookie = ResponseCookie.from(ADMIN_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        String email = requireAdmin(request);
        return Map.of(
                "authenticated", true,
                "email", email
        );
    }

    @GetMapping("/overview")
    public AdminMonitoringService.AdminOverviewResponse overview(HttpServletRequest request) {
        requireAdmin(request);
        return adminMonitoringService.getOverview();
    }

    private String requireAdmin(HttpServletRequest request) {
        String token = extractCookie(request, ADMIN_COOKIE_NAME);
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin login required");
        }
        return adminAuthService.getEmailIfValid(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin session"));
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record AdminLoginRequest(String email, String password) {}
}
