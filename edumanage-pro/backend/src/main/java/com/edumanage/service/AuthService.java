package com.edumanage.service;

import com.edumanage.config.AppProperties;
import com.edumanage.domain.Tenant;
import com.edumanage.domain.User;
import com.edumanage.repository.TenantRepository;
import com.edumanage.repository.UserRepository;
import com.edumanage.security.JwtService;
import com.edumanage.tenant.TenantContext;
import com.edumanage.web.dto.AuthDtos.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    /** Self-service tenant onboarding: creates the school + its first ADMIN user. */
    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest req) {
        String slug = req.slug().trim().toLowerCase(Locale.ROOT);
        if (tenantRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "School slug already taken");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(req.schoolName().trim())
                .slug(slug)
                .build());

        try {
            TenantContext.setTenantId(tenant.getId());

            User admin = userRepository.save(User.builder()
                    .email(req.adminEmail().trim().toLowerCase(Locale.ROOT))
                    .passwordHash(passwordEncoder.encode(req.adminPassword()))
                    .fullName(req.adminFullName().trim())
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .build());

            return tokensFor(admin, tenant);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Tenant tenant = tenantRepository.findBySlug(req.tenantSlug().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        User user = userRepository
                .findByTenantIdAndEmail(tenant.getId(), req.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return tokensFor(user, tenant);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a refresh token");
        }

        String tenantId = claims.get("tid", String.class);
        String userId = claims.getSubject();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown tenant"));
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));

        return tokensFor(user, tenant);
    }

    private AuthResponse tokensFor(User user, Tenant tenant) {
        return new AuthResponse(
                jwtService.issueAccessToken(user),
                jwtService.issueRefreshToken(user),
                "Bearer",
                appProperties.jwt().accessTokenTtl().toSeconds(),
                new UserView(user.getId(), user.getEmail(), user.getFullName(),
                             user.getRole().name(), user.getStudentId()),
                new TenantView(tenant.getId(), tenant.getName(), tenant.getSlug()));
    }
}