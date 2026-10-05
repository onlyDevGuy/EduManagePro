package com.edumanage.security;

import com.edumanage.repository.UserRepository;
import com.edumanage.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            String token = resolveBearer(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(token, request);
            }
            chain.doFilter(request, response);
        } finally {
            // Always unbind — the servlet container reuses threads.
            TenantContext.clear();
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.parse(token);
            if (jwtService.isRefreshToken(claims)) {
                return; // refresh tokens are only valid at /auth/refresh
            }
            String userId = claims.getSubject();
            String tenantId = claims.get("tid", String.class);
            if (userId == null || tenantId == null) return;

            TenantContext.setTenantId(tenantId);

            userRepository.findByIdAndTenantId(userId, tenantId).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            log.debug("Rejected JWT: {}", ex.getMessage());
        }
    }

    private String resolveBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}