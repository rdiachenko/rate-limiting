package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class FixedWindowRateLimiter {

  private final int maxCount;
  private final Duration windowDuration;
  private final Clock clock;
  private final Map<String, FixedWindow> userFixedWindow = new HashMap<>();

  /**
   * Constructs a FixedWindowRateLimiter with the specified
   * maximum request count and window duration.
   *
   * @param maxCount       The maximum number of requests a user
   *                       is allowed to make within each window.
   * @param windowDuration The duration of the window for which
   *                       the request count is valid.
   * @param clock          The clock instance to use for timing.
   */
  FixedWindowRateLimiter(int maxCount, Duration windowDuration, Clock clock) {
    this.maxCount = maxCount;
    this.windowDuration = windowDuration;
    this.clock = clock;
  }

  /**
   * Determines whether a request from the specified user ID
   * is allowed based on their activity in the current window.
   *
   * @param userId The ID of the user making the request.
   * @return true if the request is allowed, false otherwise.
   */
  boolean allowed(String userId) {
    long now = clock.millis();
    FixedWindow fixedWindow = userFixedWindow.get(userId);

    // Initialize a new fixed window for new users or
    // when the current window has expired.
    if (fixedWindow == null
        || fixedWindow.timestamp() + windowDuration.toMillis() < now) {
      fixedWindow = new FixedWindow(now, 0);
    }

    // Disallow the request if the number of requests
    // in the current window exceeds the limit.
    if (fixedWindow.count() >= maxCount) {
      return false;
    } else {
      // Increment the request count and update the window for the user.
      userFixedWindow.put(userId,
          new FixedWindow(fixedWindow.timestamp(), fixedWindow.count() + 1));
      return true;
    }
  }

  /**
   * A record representing a fixed window with a start timestamp
   * and a request count.
   */
  private record FixedWindow(long timestamp, int count) {
  }
}
