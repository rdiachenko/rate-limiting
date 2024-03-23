package com.rdiachenko.ratelimiting;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LeakyBucketRateLimiterTest {

  private static final String BOB = "Bob";
  private static final String ALICE = "Alice";

  @Test
  void allowed_burstyTraffic_acceptsAllRequestsWithinRateLimitThresholds() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 600L, 900L,
        1500L, 2000L, 2400L, 3500L, 3501L, 3502L);

    LeakyBucketRateLimiter limiter
        = new LeakyBucketRateLimiter(2, Duration.ofSeconds(1), 1, clock);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass," +
            " because bucket has 0 pending requests (water level = 0)");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=600 must pass," +
            " because bucket has capacity for 1 more request (water level = 1)");

    // 1 second passed
    assertFalse(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=900 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=1500 must pass," +
            " because bucket leaked 1 request since the last leak timestamp=0" +
            " making capacity for 1 request");

    // 2 seconds passed
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=2000 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 6 at timestamp=2400 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");

    // 3 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 7 at timestamp=3500 must pass," +
            " because bucket leaked 2 requests since the last leak timestamp=1500" +
            " making capacity for 2 requests");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 8 at timestamp=3501 must pass," +
            " because bucket has capacity for 1 more request (water level = 1)");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 9 at timestamp=3502 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");
  }

  @Test
  void allowed_requestsFromMultipleUsers_ensuresIndividualRateLimiters() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 999L, 1000L, 1000L,
        1000L, 1001L, 2001L, 2001L, 2001L, 3002L, 3003L);

    LeakyBucketRateLimiter limiter
        = new LeakyBucketRateLimiter(1, Duration.ofSeconds(2), 1, clock);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=999 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");

    // 1 second passed
    assertFalse(limiter.allowed(BOB),
        "Alice's request 3 at timestamp=1000 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");
    assertTrue(limiter.allowed(ALICE),
        "Alice's request 1 at timestamp=1000 must pass");
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 2 at timestamp=1001 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");

    // 2 seconds passed
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 3 at timestamp=2001 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=2001 must pass," +
            " because bucket leaked 1 request since the last leak timestamp=0" +
            " making capacity for 1 request");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=2001 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");

    // 3 seconds passed
    assertTrue(limiter.allowed(ALICE),
        "Alice's request 4 at timestamp=3002 must pass," +
            " because bucket leaked 1 request since the last leak timestamp=1000" +
            " making capacity for 1 request");
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 5 at timestamp=3003 must not be allowed," +
            " because bucket has reached its max capacity and no leaks occurred");
  }
}
