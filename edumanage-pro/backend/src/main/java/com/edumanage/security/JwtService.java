package com.edumanage.security;

import com.edumanage.config.AppProperties;
import com.edumanage.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final AppProperties.Jwt props;

    public JwtService(AppProperties appProperties) {
        this.props = appProperties.jwt();
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
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