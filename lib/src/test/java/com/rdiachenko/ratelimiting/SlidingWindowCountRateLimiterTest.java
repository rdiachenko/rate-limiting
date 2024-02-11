package com.rdiachenko.ratelimiting;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlidingWindowCountRateLimiterTest {

  private static final String BOB = "Bob";
  private static final String ALICE = "Alice";

  @Test
  void allowed_burstyTraffic_acceptsAllRequestsWithinRateLimitThresholds() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 999L, 1000L,
        1001L, 1002L, 1999L, 2000L);

    SlidingWindowCountRateLimiter limiter
        = new SlidingWindowCountRateLimiter(2, 1000, clock);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=999 must pass");

    // 1 second passed
    assertFalse(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=1000 must not be allowed");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=1001 must pass, because previous" +
            " fixed window weight was decreased to 0.999" +
            " which discarded request at timestamp=0");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=1002 must not be allowed");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 6 at timestamp=1999 must pass, because previous" +
            " fixed window weight was decreased to 0,001" +
            " which discarded request at timestamp=999");

    // 2 seconds passed
    assertFalse(limiter.allowed(BOB),
        "Bob's request 7 at timestamp=2000 must not be allowed");
  }

  @Test
  void allowed_requestsFromMultipleUsers_ensuresIndividualRateLimiters() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 999L, 1000L, 1000L, 1001L,
        2001L, 2001L, 2001L, 3002L, 3003L);

    SlidingWindowCountRateLimiter limiter
        = new SlidingWindowCountRateLimiter(1, 2000, clock);

    // 0 seconds passed
    assertTrue(limiter.allowed(BOB),
        "Bob's request 1 at timestamp=0 must pass");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 2 at timestamp=999 must not be allowed");

    // 1 second passed
    assertFalse(limiter.allowed(BOB),
        "Bob's request 3 at timestamp=1000 must not be allowed");
    assertTrue(limiter.allowed(ALICE),
        "Alice's request 1 at timestamp=1000 must pass");
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 2 at timestamp=1001 must not be allowed");

    // 2 seconds passed
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 3 at timestamp=2001 must not be allowed");
    assertTrue(limiter.allowed(BOB),
        "Bob's request 4 at timestamp=2001 must pass, because previous" +
            " fixed window weight was decreased to 0.9995" +
            " which discarded request at timestamp=0");
    assertFalse(limiter.allowed(BOB),
        "Bob's request 5 at timestamp=2001 must not be allowed");

    // 3 seconds passed
    assertTrue(limiter.allowed(ALICE),
        "Alice's request 4 at timestamp=3002 must pass, because previous" +
            " fixed window weight was decreased to 0.999" +
            " which discarded request at timestamp=1000");
    assertFalse(limiter.allowed(ALICE),
        "Alice's request 5 at timestamp=3003 must not be allowed");
  }
}
