package com.andrei.demo.util;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Slf4j
@AllArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/login", "/forgot-password", "/reset-password");
    private static final Set<String> ADMIN_PREFIXES = Set.of("/person", "/equipment", "/loan");

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow CORS preflight and public endpoints
        if ("OPTIONS".equalsIgnoreCase(method) || PUBLIC_PATHS.contains(path)) {
            log.debug("Skipping JWT filter for path: {} method: {}", path, method);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("Missing or malformed Authorization header for path: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.checkClaims(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Role-based authorization for admin-only routes
            if (isAdminRoute(path)) {
                String role = jwtUtil.getRoleFromToken(token);
                if (!"ADMIN".equals(role)) {
                    log.warn("Forbidden: role '{}' tried to access admin route '{}'", role, path);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isAdminRoute(String path) {
        return ADMIN_PREFIXES.stream().anyMatch(path::startsWith);
    }
}