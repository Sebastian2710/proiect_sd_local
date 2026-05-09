package com.andrei.demo.assistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RateLimiterServiceTest {

    @Test
    void enforce_allowsCallsUpToCapacity() {
        RateLimiterService limiter = new RateLimiterService(3, Duration.ofHours(1));
        limiter.enforceLimit("user1", "op");
        limiter.enforceLimit("user1", "op");
        limiter.enforceLimit("user1", "op");
        // 4th must throw
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> limiter.enforceLimit("user1", "op"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void enforce_separateBucketsPerUser() {
        RateLimiterService limiter = new RateLimiterService(2, Duration.ofHours(1));
        limiter.enforceLimit("user1", "op");
        limiter.enforceLimit("user1", "op");
        assertThrows(ResponseStatusException.class,
                () -> limiter.enforceLimit("user1", "op"));
        // user2 has its own bucket
        assertDoesNotThrow(() -> limiter.enforceLimit("user2", "op"));
        assertDoesNotThrow(() -> limiter.enforceLimit("user2", "op"));
    }

    @Test
    void clearAll_resetsBuckets() {
        RateLimiterService limiter = new RateLimiterService(1, Duration.ofHours(1));
        limiter.enforceLimit("user1", "op");
        assertThrows(ResponseStatusException.class,
                () -> limiter.enforceLimit("user1", "op"));
        limiter.clearAll();
        assertDoesNotThrow(() -> limiter.enforceLimit("user1", "op"));
    }

    @Test
    void enforce_nullUserKey_throwsIllegalArgument() {
        RateLimiterService limiter = new RateLimiterService(5, Duration.ofHours(1));
        assertThrows(IllegalArgumentException.class,
                () -> limiter.enforceLimit(null, "op"));
        assertThrows(IllegalArgumentException.class,
                () -> limiter.enforceLimit("  ", "op"));
    }

    @Test
    void constructor_validatesArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterService(0, Duration.ofHours(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterService(-1, Duration.ofHours(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterService(5, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterService(5, Duration.ofHours(-1)));
    }
}