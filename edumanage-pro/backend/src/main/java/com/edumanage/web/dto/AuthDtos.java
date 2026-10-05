package com.edumanage.web.dto;

import jakarta.validation.constraints.*;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterTenantRequest(
            @NotBlank @Size(max = 200) String schoolName,
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,78}[a-z0-9]$",
                    message = "slug must be 3-80 chars, lowercase alphanumeric or hyphen") String slug,
            @NotBlank @Email String adminEmail,
            @NotBlank @Size(min = 10, max = 100) String adminPassword,
            @NotBlank @Size(max = 200) String adminFullName) {}

    public record LoginRequest(
            @NotBlank String tenantSlug,
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserView(String id, String email, String fullName, String role, String studentId) {}

    public record TenantView(String id, String name, String slug) {}

    public record AuthResponse(String accessToken, String refreshToken, String tokenType,
                               long expiresInSeconds, UserView user, TenantView tenant) {}
}