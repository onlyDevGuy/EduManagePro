package com.edumanage.security;

import com.edumanage.config.AppProperties;
import com.edumanage.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUnitTest {

    @Test
    void jwtSecretMustBeLongEnough() {
        assertThatThrownBy(() -> JwtService.requireStrongSecret(null, false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> JwtService.requireStrongSecret("too-short", false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeholderJwtSecretIsRefusedInProductionOnly() {
        String devDefault = "dev-only-secret-change-me-please-32-bytes-minimum!!";
        assertThat(JwtService.requireStrongSecret(devDefault, false)).isEqualTo(devDefault);
        assertThatThrownBy(() -> JwtService.requireStrongSecret(devDefault, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
        String real = "N0t4Pl4c3h0ld3r-but-a-real-random-secret-value";
        assertThat(JwtService.requireStrongSecret(real, true)).isEqualTo(real);
    }

    @Test
    void lockoutCountsFailuresPerAccountAndExpires() {
        MutableClock clock = new MutableClock();
        LoginAttemptService attempts = new LoginAttemptService(
                new AppProperties(null, null, null, null, new AppProperties.Login(3, Duration.ofMinutes(10))), clock);

        assertThat(attempts.recordFailure("school", "a@x.test")).isFalse();
        assertThat(attempts.recordFailure("school", "a@x.test")).isFalse();
        assertThat(attempts.recordFailure("SCHOOL", " A@X.TEST ")).isTrue(); // same account, normalised
        assertThat(attempts.lockedFor("school", "a@x.test")).contains(Duration.ofMinutes(10));
        assertThat(attempts.lockedFor("school", "b@x.test")).isEmpty();

        clock.advance(Duration.ofMinutes(10));
        assertThat(attempts.lockedFor("school", "a@x.test")).isEmpty();
        // After the lock expires the count starts again from zero.
        assertThat(attempts.recordFailure("school", "a@x.test")).isFalse();
    }

    @Test
    void successResetsTheFailureCount() {
        LoginAttemptService attempts = new LoginAttemptService(
                new AppProperties(null, null, null, null, new AppProperties.Login(2, Duration.ofMinutes(5))), new MutableClock());

        attempts.recordFailure("school", "a@x.test");
        attempts.recordSuccess("school", "a@x.test");
        assertThat(attempts.recordFailure("school", "a@x.test")).isFalse();
    }
}
