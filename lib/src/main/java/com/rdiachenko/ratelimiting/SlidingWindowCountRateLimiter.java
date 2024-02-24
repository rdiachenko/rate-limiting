package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class SlidingWindowCountRateLimiter {

  private final int maxCount;
  private final Duration windowDuration;
  private final Clock clock;
  private final Map<String, SlidingWindow> userSlidingWindow = new HashMap<>();

  /**
   * Constructs a SlidingWindowCountRateLimiter with
   * specified maximum count, window duration, and clock.
   *
   * @param maxCount       The maximum number of requests
   *                       allowed within the window duration.
   * @param windowDuration The duration of the sliding window.
   * @param clock          The clock instance for determining the current time.
   */
  public SlidingWindowCountRateLimiter(int maxCount, Duration windowDuration, Clock clock) {
    this.maxCount = maxCount;
    this.windowDuration = windowDuration;
    this.clock = clock;
  }

  /**
   * Determines if a request from the specified user ID is allowed
   * based on their activity within the current sliding window.
   *
   * @param userId The ID of the user making the request.
   * @return true if the request is allowed, false otherwise.
   */
  public boolean allowed(String userId) {
    long now = clock.millis();

    // Initialize an empty sliding window for new users
    // or retrieve the existing one.
    SlidingWindow slidingWindow = userSlidingWindow.computeIfAbsent(userId,
        k -> new SlidingWindow(new FixedWindow(now, 0),
            new FixedWindow(now, 0)));

    FixedWindow currentFixedWindow = slidingWindow.currentFixedWindow();
    FixedWindow previousFixedWindow = slidingWindow.previousFixedWindow();

    // Transition to a new fixed window when the current one expires.
    if (currentFixedWindow.timestamp() + windowDuration.toMillis() < now) {
      previousFixedWindow = currentFixedWindow;
      currentFixedWindow = new FixedWindow(now, 0);
      userSlidingWindow.put(userId,
          new SlidingWindow(previousFixedWindow, currentFixedWindow));
    }

    // Weight calculation for the previous window.
    long slidingWindowStart = Math.max(0, now - windowDuration.toMillis());
    long previousFixedWindowEnd =
        previousFixedWindow.timestamp() + windowDuration.toMillis();
    // Weight of the previous window based on overlap with the sliding window.
    double previousFixedWindowWeight =
        Math.max(0, previousFixedWindowEnd - slidingWindowStart)
            / (double) windowDuration.toMillis();

    // Calculate total request count within the sliding window.
    int count = (int) (previousFixedWindow.count()
        * previousFixedWindowWeight
        + currentFixedWindow.count());

    // Check if the request count within the sliding window exceeds the limit.
    // If so, reject the request; otherwise, update the request count
    // in the current fixed window and allow the request.
    if (count >= maxCount) {
      return false;
    } else {
      currentFixedWindow = new FixedWindow(currentFixedWindow.timestamp(),
          currentFixedWindow.count() + 1);
      userSlidingWindow.put(userId,
          new SlidingWindow(previousFixedWindow, currentFixedWindow));
      return true;
    }
  }

  /**
   * Represents a sliding window consisting of a previous
   * and a current fixed window.
   */
  private record SlidingWindow(FixedWindow previousFixedWindow,
                               FixedWindow currentFixedWindow) {
  }

  /**
   * Represents a fixed window with a timestamp marking
   * its start and a count of requests.
   */
  private record FixedWindow(long timestamp, int count) {
  }
}
