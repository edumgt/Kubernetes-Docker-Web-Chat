package org.example.profileservice.util;


import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class CookieJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CookieJwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public CookieJwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = null;

        String path = request.getRequestURI();

        if(path.startsWith("/profile/")){
            logger.info("bypassing jwt filter for public endpoint: {}" , path);
            filterChain.doFilter(request,response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            logger.warn("No JWT token found in cookies, redirecting to login.");
            if (!request.getRequestURI().contains("/auth/login")) {
                response.sendRedirect("/auth/login");
                return;
            }
        } else {
            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                String email = claims.getSubject();
                logger.info("Token subject (email): {}", email);
                if (jwtUtil.isTokenValid(token, email)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("Authentication set for user: {}", email);
                } else {
                    logger.warn("JWT is invalid for email: {}", email);
                    SecurityContextHolder.clearContext();
                    response.sendRedirect("/auth/login");
                    return;
                }
            } catch (Exception ex) {
                logger.error("JWT validation error: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
                response.sendRedirect("/auth/login");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
