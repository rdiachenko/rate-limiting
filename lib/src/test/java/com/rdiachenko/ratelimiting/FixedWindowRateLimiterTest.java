package com.rdiachenko.ratelimiting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FixedWindowRateLimiterTest {

    @Test
    void testLimiter() throws Exception {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 2000);
        String bob = "Bob";
        String alice = "Alice";

        assertTrue(limiter.allowed(bob), "First Bob's request must pass");
        assertFalse(limiter.allowed(bob), "Second Bob's request must not be allowed");

        Thread.sleep(1000); // 1 second passed
        assertFalse(limiter.allowed(bob), "Bob's fixed window is not reset yet, request must not be allowed");
        assertTrue(limiter.allowed(alice), "First Alice's request must pass");
        assertFalse(limiter.allowed(alice), "Second Alice's request must not be allowed");

        Thread.sleep(1000); // 2 seconds passed
        assertFalse(limiter.allowed(alice), "Alice's fixed window is not reset yet, request must not be allowed");
        assertTrue(limiter.allowed(bob), "Bob's fixed window is reset, request must pass");
        assertFalse(limiter.allowed(bob), "Second Bob's request must not be allowed");

        Thread.sleep(1000); // 3 seconds passed
        assertTrue(limiter.allowed(alice), "Alice's fixed window is reset, request must pass");
        assertFalse(limiter.allowed(alice), "Second Alice's request must not be allowed");
    }
}
