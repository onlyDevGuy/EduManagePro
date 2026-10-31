package com.edumanage.security;

import com.edumanage.config.AppProperties;
import com.edumanage.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

@Service
public class JwtService {

    private final SecretKey key;
    private final AppProperties.Jwt props;

    public JwtService(AppProperties appProperties, Environment environment) {
        this.props = appProperties.jwt();
        boolean production = environment.matchesProfiles("prod");
        this.key = Keys.hmacShaKeyFor(requireStrongSecret(props.secret(), production).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fails startup rather than signing tokens with a missing, short or placeholder secret:
     * anyone who knows the secret can mint a token for any user in any school. The
     * checked-in development secret is only accepted outside the prod profile.
     */
    static String requireStrongSecret(String secret, boolean production) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a random value of at least 32 bytes (e.g. `openssl rand -base64 48`).");
        }
        String lower = secret.toLowerCase(Locale.ROOT);
        if (production && (lower.contains("change-me") || lower.contains("changeme"))) {
            throw new IllegalStateException(
                    "JWT_SECRET is still a placeholder value. Generate a real secret for this environment.");
        }
        return secret;
    }

    public String issueAccessToken(User user) {
        return issue(user, props.accessTokenTtl(), "access");
    }

    public String issueRefreshToken(User user) {
        return issue(user, props.refreshTokenTtl(), "refresh");
    }

    private String issue(User user, java.time.Duration ttl, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(user.getId())
                .claim("tid", user.getTenantId())
                .claim("role", user.getRole().name())
                .claim("typ", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException when the token is invalid, expired, or has the wrong issuer. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("typ", String.class));
    }

    public static boolean isMalformed(String token) {
        return token == null || token.isBlank();
    }
}