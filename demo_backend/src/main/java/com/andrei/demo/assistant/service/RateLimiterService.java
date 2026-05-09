package com.andrei.demo.assistant.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Per-user token-bucket rate limiter backed by Bucket4j.
 *
 * <p>Buckets are kept in memory; sized by {@code assistant.ratelimit.capacity}
 * and refilled fully every {@code assistant.ratelimit.refill-period}. The map
 * is bounded only by the number of distinct users that ever hit a rate-limited
 * endpoint, so for production-grade deployments a TTL-bounded cache (e.g.
 * Caffeine) or external storage (Redis) would replace the {@link
 * ConcurrentHashMap}. For a single-instance student-project deployment this
 * is fine — buckets live for the JVM's lifetime and are reset on restart.
 *
 * <p>{@link #enforceLimit} is the single public entry point; it either
 * consumes a token and returns, or throws a {@link ResponseStatusException}
 * with HTTP 429 carrying a human-readable retry hint.
 */
@Service
@Slf4j
public class RateLimiterService {

    private final long capacity;
    private final Duration refillPeriod;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${assistant.ratelimit.capacity}") long capacity,
            @Value("${assistant.ratelimit.refill-period}") Duration refillPeriod
    ) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1, got " + capacity);
        }
        if (refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("refillPeriod must be positive, got " + refillPeriod);
        }
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        log.info("RateLimiterService initialised: capacity={}, refillPeriod={}", capacity, refillPeriod);
    }

    /**
     * Consume one token from {@code userKey}'s bucket.
     *
     * @param userKey   identifier for the bucket — typically the user's email
     * @param operation human-readable operation name, used in error messages
     * @throws ResponseStatusException with status 429 if the bucket is empty
     */
    public void enforceLimit(String userKey, String operation) {
        if (userKey == null || userKey.isBlank()) {
            throw new IllegalArgumentException("userKey must not be null or blank");
        }
        Bucket bucket = buckets.computeIfAbsent(userKey, k -> newBucket());
        if (!bucket.tryConsume(1)) {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(
                    bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill());
            log.warn("Rate limit exceeded for user '{}' on operation '{}'; retry in ~{}s",
                    userKey, operation, waitSeconds);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded for " + operation
                            + ". Try again in approximately " + waitSeconds + " seconds.");
        }
    }

    /** Test/admin hook: drop all buckets. Buckets re-initialise on next access. */
    public void clearAll() {
        buckets.clear();
        log.debug("Cleared all rate-limit buckets");
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, refillPeriod)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}