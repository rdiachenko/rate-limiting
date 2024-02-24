package com.rdiachenko.ratelimiting;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static com.rdiachenko.ratelimiting.TokenBucketRateLimiter.RefillStrategy.GREEDY;
import static com.rdiachenko.ratelimiting.TokenBucketRateLimiter.RefillStrategy.INTERVALLY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenBucketRateLimiterTest {

  private static final String BOB = "Bob";
  private static final String ALICE = "Alice";

  @Test
  void allowed_burstyTraffic_acceptsAllRequestsWithinRateLimitThresholdsUsingIntervallyRefill() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 999L, 1000L,
        1001L, 1002L, 1499L, 2000L);

    TokenBucketRateLimiter limiter
        = new TokenBucketRateLimiter(2, Duration.ofSeconds(1), 2, clock, INTERVALLY);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass," +
            " because bucket has 2 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=999 must pass," +
            " because bucket has 1 token available");

    // 1 second passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=1000 must pass," +
            " because bucket was refilled with 2 tokens" +
            " since the last refill at timestamp=0" +
            " and now has 2 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=1001 must pass," +
            " because bucket has 1 token available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=1002 must not be allowed," +
            " because bucket has 0 tokens available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 6 at timestamp=1499 must not be allowed," +
            " because bucket has 0 tokens available");

    // 2 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 7 at timestamp=2000 must pass," +
            " because bucket was refilled with 2 tokens" +
            " since the last refill at timestamp=1000" +
            " and now has 2 tokens available");
  }

  @Test
  void allowed_burstyTraffic_acceptsAllRequestsWithinRateLimitThresholdsUsingGreedyRefill() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 999L, 1000L,
        1001L, 1002L, 1499L, 2000L);

    TokenBucketRateLimiter limiter
        = new TokenBucketRateLimiter(2, Duration.ofSeconds(1), 2, clock, INTERVALLY);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass," +
            " because bucket has 2 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=999 must pass," +
            " because bucket was refilled with 1 token" +
            " since the last refill at timestamp=0" +
            " and now has 2 tokens available");

    // 1 second passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=1000 must pass," +
            " because bucket was refilled with 1 token" +
            " since the last refill at timestamp=500" +
            " and now has 2 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=1001 must pass," +
            " because bucket has 1 token available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=1002 must not be allowed," +
            " because bucket has 0 tokens available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 6 at timestamp=1499 must not be allowed," +
            " because bucket has 0 tokens available");

    // 2 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 7 at timestamp=2000 must pass," +
            " because bucket was refilled with 2 tokens" +
            " since the last refill at timestamp=1000" +
            " and now has 2 tokens available");
  }

  @Test
  void allowed_burstyTraffic_acceptsAllAccumulatedRequestsWithinRateLimitThresholds() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 1L, 4001L, 4002L, 4003L, 4004L, 4005L);

    TokenBucketRateLimiter limiter
        = new TokenBucketRateLimiter(4, Duration.ofSeconds(1), 1, clock, INTERVALLY);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass," +
            " because bucket has 1 token available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=1 must not be allowed," +
            " because bucket has 0 tokens available");

    // 4 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=4001 must pass," +
            " because bucket accumulated 4 tokens" +
            " since the last refill at timestamp=0" +
            " and now has 4 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=4002 must pass," +
            " because bucket has 3 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=4003 must pass," +
            " because bucket has 2 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 6 at timestamp=4004 must pass," +
            " because bucket has 1 token available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 7 at timestamp=4005 must not be allowed," +
            " because bucket has 0 tokens available");
  }

  @Test
  void allowed_requestsFromMultipleUsers_ensuresIndividualRateLimiters() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 999L, 1000L, 1000L,
        1000L, 1001L, 2001L, 2001L, 2001L, 3002L, 3003L);

    TokenBucketRateLimiter limiter
        = new TokenBucketRateLimiter(1, Duration.ofSeconds(2), 1, clock, GREEDY);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=999 must not be allowed," +
            " because bucket has 0 tokens available");

    // 1 second passed
    assertFalse(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=1000 must not be allowed," +
            " because bucket has 0 tokens available");
    assertTrue(limiter.allowed(ALICE),
        "Alice's request 1 at timestamp=1000 must pass");
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 2 at timestamp=1001 must not be allowed," +
            " because bucket has 0 tokens available");

    // 2 seconds passed
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 3 at timestamp=2001 must not be allowed," +
            " because bucket has 0 tokens available");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=2001 must pass," +
            " because bucket was refilled with 1 token" +
            " since the last refill at timestamp=0" +
            " and now has 1 token available");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=2001 must not be allowed," +
            " because bucket has 0 tokens available");

    // 3 seconds passed
    assertTrue(limiter.allowed(ALICE),
        "Alice's request 4 at timestamp=3002 must pass," +
            " because bucket was refilled with 1 token" +
            " since the last refill at timestamp=1000" +
            " and now has 1 token available");
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 5 at timestamp=3003 must not be allowed," +
            " because bucket has 0 tokens available");
  }
}
