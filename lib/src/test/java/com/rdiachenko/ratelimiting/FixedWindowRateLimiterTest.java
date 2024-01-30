package com.rdiachenko.ratelimiting;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FixedWindowRateLimiterTest {

  @Test
  void testLimiter() {
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L, 0L, 1000L, 1000L, 1000L,
        2001L, 2001L, 2001L, 3001L, 3001L);

    FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 2000, clock);
    String bob = "Bob";
    String alice = "Alice";

    assertTrue(limiter.allowed(bob),
        "First Bob's request must pass");
    assertFalse(limiter.allowed(bob),
        "Second Bob's request must not be allowed");

    // 1 second passed
    assertFalse(limiter.allowed(bob),
        "Bob's fixed window is not reset yet, request must not be allowed");
    assertTrue(limiter.allowed(alice),
        "First Alice's request must pass");
    assertFalse(limiter.allowed(alice),
        "Second Alice's request must not be allowed");

    // 2 seconds passed
    assertFalse(limiter.allowed(alice),
        "Alice's fixed window is not reset yet, request must not be allowed");
    assertTrue(limiter.allowed(bob),
        "Bob's fixed window is reset, request must pass");
    assertFalse(limiter.allowed(bob),
        "Second Bob's request must not be allowed");

    // 3 seconds passed
    assertTrue(limiter.allowed(alice),
        "Alice's fixed window is reset, request must pass");
    assertFalse(limiter.allowed(alice),
        "Second Alice's request must not be allowed");
  }
}
