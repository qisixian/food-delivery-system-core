package com.sky.utils;

import java.time.Duration;

public final class LogRateLimiter {

    // 被限频的调用复用同一个结果，避免频繁创建对象。
    private static final Decision SUPPRESSED = new Decision(false, 0);

    private final long intervalNanos;

    private boolean initialized;
    private long lastGrantedNanos;
    private long suppressedCount;

    public LogRateLimiter(Duration interval) {
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        this.intervalNanos = interval.toNanos();
        this.initialized = false;
        this.suppressedCount = 0;
    }

    public synchronized Decision tryAcquire() {
        long now = System.nanoTime();

        if (initialized && now - lastGrantedNanos < intervalNanos) {
            suppressedCount++;
            return SUPPRESSED;
        }

        long skipped = suppressedCount;

        initialized = true;
        lastGrantedNanos = now;
        suppressedCount = 0;

        return new Decision(true, skipped);
    }

    public record Decision(
            boolean allowed,
            long suppressedCount
    ) {
    }
}