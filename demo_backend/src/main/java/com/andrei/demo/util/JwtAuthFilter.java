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

    private static final Set<String> PUBLIC_PATHS =
            Set.of("/login", "/forgot-password", "/reset-password", "/register");

    // Any authenticated user (student or admin) may access these
    private static final Set<String> STUDENT_ALLOWED_PREFIXES =
            Set.of("/loan/my", "/loan/request", "/assistant/recommend");

    // Admin-only prefixes (checked last)
    private static final Set<String> ADMIN_ONLY_PREFIXES =
            Set.of("/person", "/equipment", "/loan");

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

        // Validate JWT presence
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

            // Student-accessible paths: any valid JWT is enough
            if (STUDENT_ALLOWED_PREFIXES.stream().anyMatch(path::startsWith)) {
                log.debug("Student-accessible path allowed: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // GET /equipment is readable by any authenticated user
            if (path.startsWith("/equipment") && "GET".equalsIgnoreCase(method)) {
                log.debug("Allowing authenticated GET on /equipment");
                filterChain.doFilter(request, response);
                return;
            }

            // Everything else under admin prefixes requires ADMIN role
            if (ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith)) {
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
}