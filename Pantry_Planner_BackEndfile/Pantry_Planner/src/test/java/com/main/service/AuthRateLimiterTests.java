package com.main.service;

import com.main.exception.AuthRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTests {

    @Test
    void limitsEachAuthenticationActionByClientAddress() {
        AuthRateLimiter limiter = new AuthRateLimiter(
                Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC),
                2,
                Duration.ofMinutes(1),
                1,
                Duration.ofHours(1),
                "");
        MockHttpServletRequest firstClient = requestFrom("203.0.113.10");

        limiter.checkLogin(firstClient);
        limiter.checkLogin(firstClient);
        assertThatThrownBy(() -> limiter.checkLogin(firstClient))
                .isInstanceOf(AuthRateLimitExceededException.class)
                .satisfies(exception -> assertThat(
                        ((AuthRateLimitExceededException) exception).getRetryAfterSeconds())
                        .isEqualTo(60));
        assertThatCode(() -> limiter.checkLogin(requestFrom("203.0.113.11")))
                .doesNotThrowAnyException();

        limiter.checkRegistration(firstClient);
        assertThatThrownBy(() -> limiter.checkRegistration(firstClient))
                .isInstanceOf(AuthRateLimitExceededException.class);

        assertThatCode(() -> limiter.checkLogin(requestFrom("203.0.113.12")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsRequestsAgainWhenTheWindowEnds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        AuthRateLimiter limiter = new AuthRateLimiter(
                clock,
                1,
                Duration.ofMillis(1_500),
                1,
                Duration.ofHours(1),
                "");
        MockHttpServletRequest request = requestFrom("203.0.113.10");

        limiter.checkLogin(request);
        clock.advance(Duration.ofMillis(1));
        assertThatThrownBy(() -> limiter.checkLogin(request))
                .isInstanceOf(AuthRateLimitExceededException.class)
                .satisfies(exception -> assertThat(
                        ((AuthRateLimitExceededException) exception).getRetryAfterSeconds())
                        .isEqualTo(2));

        clock.advance(Duration.ofMillis(1_499));
        assertThatCode(() -> limiter.checkLogin(request)).doesNotThrowAnyException();
    }

    @Test
    void usesTheConfiguredTrustedClientAddressHeader() {
        AuthRateLimiter limiter = new AuthRateLimiter(
                Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC),
                2,
                Duration.ofMinutes(1),
                1,
                Duration.ofHours(1),
                "CF-Connecting-IP");
        MockHttpServletRequest firstRequest = requestFrom("10.0.0.1");
        firstRequest.addHeader("CF-Connecting-IP", "198.51.100.20");
        MockHttpServletRequest secondRequest = requestFrom("10.0.0.2");
        secondRequest.addHeader("CF-Connecting-IP", "198.51.100.20");

        limiter.checkRegistration(firstRequest);
        assertThatThrownBy(() -> limiter.checkRegistration(secondRequest))
                .isInstanceOf(AuthRateLimitExceededException.class);
    }

    private static MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }

    private static final class MutableClock extends Clock {

        private Instant currentInstant;

        private MutableClock(Instant currentInstant) {
            this.currentInstant = currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
