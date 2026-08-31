package com.main.service;

import com.main.exception.AuthRateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();

    private final Clock clock;
    private final Limit loginLimit;
    private final Limit registrationLimit;
    private final String trustedClientIpHeader;
    private final Map<ClientAction, AttemptWindow> windows = new HashMap<>();
    private final Object windowLock = new Object();
    private long nextCleanupAt;

    public AuthRateLimiter(
            Clock clock,
            @Value("${app.auth.rate-limit.login.max-attempts:20}") int loginMaxAttempts,
            @Value("${app.auth.rate-limit.login.window:PT1M}") Duration loginWindow,
            @Value("${app.auth.rate-limit.registration.max-attempts:5}") int registrationMaxAttempts,
            @Value("${app.auth.rate-limit.registration.window:PT1H}") Duration registrationWindow,
            @Value("${app.auth.trusted-client-ip-header:}") String trustedClientIpHeader) {
        this.clock = clock;
        this.loginLimit = new Limit(loginMaxAttempts, loginWindow);
        this.registrationLimit = new Limit(registrationMaxAttempts, registrationWindow);
        this.trustedClientIpHeader = trustedClientIpHeader.trim();
    }

    public void checkLogin(HttpServletRequest request) {
        check("login", clientAddress(request), loginLimit);
    }

    public void checkRegistration(HttpServletRequest request) {
        check("registration", clientAddress(request), registrationLimit);
    }

    private void check(String action, String clientAddress, Limit limit) {
        long now = clock.millis();
        ClientAction key = new ClientAction(action, clientAddress);
        synchronized (windowLock) {
            cleanupExpiredWindows(now);
            AttemptWindow current = windows.get(key);
            if (current == null && windows.size() >= MAX_TRACKED_KEYS) {
                throw new AuthRateLimitExceededException(Math.max(1, limit.window().toSeconds()));
            }
            if (current == null || now - current.startedAt() >= limit.window().toMillis()) {
                windows.put(key, new AttemptWindow(now, 1));
                return;
            }
            if (current.attempts() >= limit.maxAttempts()) {
                long retryAfterMillis = limit.window().toMillis() - (now - current.startedAt());
                long retryAfterSeconds = Math.max(1, (retryAfterMillis + 999) / 1_000);
                throw new AuthRateLimitExceededException(retryAfterSeconds);
            }
            windows.put(key, new AttemptWindow(current.startedAt(), current.attempts() + 1));
        }
    }

    private String clientAddress(HttpServletRequest request) {
        if (!trustedClientIpHeader.isEmpty()) {
            String forwardedAddress = request.getHeader(trustedClientIpHeader);
            if (forwardedAddress != null && !forwardedAddress.isBlank()) {
                return boundedAddress(forwardedAddress);
            }
        }
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank()
                ? "unknown"
                : boundedAddress(remoteAddress);
    }

    private void cleanupExpiredWindows(long now) {
        if (now < nextCleanupAt) {
            return;
        }
        nextCleanupAt = now + CLEANUP_INTERVAL_MILLIS;

        windows.entrySet().removeIf(entry -> {
            Limit limit = entry.getKey().action().equals("login")
                    ? loginLimit
                    : registrationLimit;
            return now - entry.getValue().startedAt() >= limit.window().toMillis();
        });
    }

    private static String boundedAddress(String address) {
        String normalized = address.trim();
        return normalized.substring(0, Math.min(normalized.length(), 128));
    }

    private record ClientAction(String action, String clientAddress) {
    }

    private record AttemptWindow(long startedAt, int attempts) {
    }

    private record Limit(int maxAttempts, Duration window) {
        private Limit {
            if (maxAttempts < 1 || window.isZero() || window.isNegative()) {
                throw new IllegalArgumentException("Authentication rate limits must be positive");
            }
        }
    }
}
