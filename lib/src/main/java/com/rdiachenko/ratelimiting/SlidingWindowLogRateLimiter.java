package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.time.Duration;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SlidingWindowLogRateLimiter {

  private final int maxCount;
  private final Duration windowDuration;
  private final Clock clock;
  private final Map<String, Deque<Long>> userSlidingWindow = new HashMap<>();

  /**
   * Constructs a SlidingWindowLogRateLimiter with the specified
   * maximum request count, window duration, and clock.
   *
   * @param maxCount       The maximum number of requests a user
   *                       is allowed within the window duration.
   * @param windowDuration The duration of the sliding window.
   * @param clock          The clock instance to use for timing purposes.
   */
  public SlidingWindowLogRateLimiter(int maxCount, Duration windowDuration, Clock clock) {
    this.maxCount = maxCount;
    this.windowDuration = windowDuration;
    this.clock = clock;
  }

  /**
   * Determines whether a request from the specified user ID
   * is allowed based on the number of requests within the current sliding window.
   *
   * @param userId The ID of the user making the request.
   * @return true if the request is allowed, false otherwise.
   */
  public boolean allowed(String userId) {
    long now = clock.millis();

    // Initialize an empty sliding window for new users
    // or retrieve the existing one.
    Deque<Long> slidingWindow = userSlidingWindow
        .computeIfAbsent(userId, k -> new LinkedList<>());

    // Remove timestamps that are outside the current sliding window.
    while (!slidingWindow.isEmpty()
        && slidingWindow.getFirst() + windowDuration.toMillis() < now) {
      slidingWindow.removeFirst();
    }

    // Check if the request count within the window exceeds the limit.
    // If so, reject the request; otherwise, add the current
    // request's timestamp to the window and allow it.
    if (slidingWindow.size() >= maxCount) {
      return false;
    } else {
      slidingWindow.addLast(now);
      return true;
    }
  }
}
