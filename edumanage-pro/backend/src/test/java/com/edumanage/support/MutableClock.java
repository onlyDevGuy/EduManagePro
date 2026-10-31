package com.edumanage.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can move forward, e.g. to let an account lockout expire. */
public class MutableClock extends Clock {

    private volatile Instant now = Instant.parse("2026-01-15T08:00:00Z");

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override public Instant instant() { return now; }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
}
