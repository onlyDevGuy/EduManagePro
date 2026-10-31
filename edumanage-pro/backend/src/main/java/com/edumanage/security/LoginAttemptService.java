package com.edumanage.security;

import com.edumanage.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an account for a while after repeated failed sign-ins, so passwords cannot be
 * guessed by brute force. Keyed by school + email; kept in memory, which is enough for
 * a single instance (a shared store such as Redis would be needed when scaling out).
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;

    public LoginAttemptService(AppProperties appProperties, Clock clock) {
        this.maxFailures = appProperties.login().maxFailures();
        this.lockout = appProperties.login().lockout();
        this.clock = clock;
    }

    /** When the account is locked, how long until it opens again. */
    public Optional<Duration> lockedFor(String tenantSlug, String email) {
        Attempts a = attempts.get(key(tenantSlug, email));
        if (a == null || a.lockedUntil() == null) return Optional.empty();
        Duration remaining = Duration.between(clock.instant(), a.lockedUntil());
        return remaining.isNegative() || remaining.isZero() ? Optional.empty() : Optional.of(remaining);
    }

    /** Records a failure and returns true if this one locked the account. */
    public boolean recordFailure(String tenantSlug, String email) {
        Instant now = clock.instant();
        Attempts updated = attempts.compute(key(tenantSlug, email), (k, previous) -> {
            boolean expiredLock = previous != null && previous.lockedUntil() != null && !now.isBefore(previous.lockedUntil());
            int failures = (previous == null || expiredLock ? 0 : previous.failures()) + 1;
            return failures >= maxFailures
                    ? new Attempts(0, now.plus(lockout))
                    : new Attempts(failures, null);
        });
        return updated.lockedUntil() != null;
    }

    public void recordSuccess(String tenantSlug, String email) {
        attempts.remove(key(tenantSlug, email));
    }

    private static String key(String tenantSlug, String email) {
        return tenantSlug.trim().toLowerCase(Locale.ROOT) + "|" + email.trim().toLowerCase(Locale.ROOT);
    }
}
