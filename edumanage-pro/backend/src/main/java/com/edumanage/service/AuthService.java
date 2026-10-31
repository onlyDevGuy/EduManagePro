package com.edumanage.service;

import com.edumanage.config.AppProperties;
import com.edumanage.domain.Tenant;
import com.edumanage.domain.User;
import com.edumanage.repository.TenantRepository;
import com.edumanage.repository.UserRepository;
import com.edumanage.security.JwtService;
import com.edumanage.security.LoginAttemptService;
import com.edumanage.tenant.TenantContext;
import com.edumanage.web.dto.AuthDtos.*;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final LoginAttemptService loginAttempts;
    private final String dummyHash;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       AppProperties appProperties, LoginAttemptService loginAttempts) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

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
        String slug = req.tenantSlug().trim().toLowerCase(Locale.ROOT);
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        loginAttempts.lockedFor(slug, email).ifPresent(remaining -> {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed sign-in attempts. Try again in " + Math.max(1, remaining.toMinutes()) + " minutes.");
        });

        Optional<Tenant> tenant = tenantRepository.findBySlug(slug);
        Optional<User> user = tenant.flatMap(t -> userRepository.findByTenantIdAndEmail(t.getId(), email));

        // Always run one bcrypt comparison, so an unknown school or email takes as long
        // as a wrong password and response times do not reveal which accounts exist.
        boolean passwordMatches = passwordEncoder.matches(req.password(),
                user.map(User::getPasswordHash).orElse(dummyHash));

        if (user.isEmpty() || !passwordMatches || !user.get().isEnabled()) {
            loginAttempts.recordFailure(slug, email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        loginAttempts.recordSuccess(slug, email);
        return tokensFor(user.get(), tenant.get());
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
                .filter(User::isEnabled) // a disabled account cannot keep renewing its session
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