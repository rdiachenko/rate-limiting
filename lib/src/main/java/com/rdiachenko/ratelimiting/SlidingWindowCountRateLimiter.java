package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class SlidingWindowCountRateLimiter {

  private final int maxCount;
  private final long windowLengthMillis;
  private final Clock clock;
  private final Map<String, SlidingWindow> userSlidingWindow = new HashMap<>();

  SlidingWindowCountRateLimiter(int maxCount, long windowLengthMillis, Clock clock) {
    this.maxCount = maxCount;
    this.windowLengthMillis = windowLengthMillis;
    this.clock = clock;
  }

  boolean allowed(String userId) {
    long now = clock.millis();

    // Initialize an empty sliding window for new users or retrieve existing one.
    SlidingWindow slidingWindow = userSlidingWindow.computeIfAbsent(userId,
        k -> new SlidingWindow(new FixedWindow(now, 0),
            new FixedWindow(now, 0)));

    FixedWindow currentFixedWindow = slidingWindow.currentFixedWindow();
    FixedWindow previousFixedWindow = slidingWindow.previousFixedWindow();

    // Transition to a new fixed window when the current one expires.
    if (currentFixedWindow.timestamp() + windowLengthMillis < now) {
      previousFixedWindow = currentFixedWindow;
      currentFixedWindow = new FixedWindow(now, 0);
      userSlidingWindow.put(userId,
          new SlidingWindow(previousFixedWindow, currentFixedWindow));
    }

    // Weight calculation for the previous window.
    long slidingWindowStart = Math.max(0, now - windowLengthMillis);
    long previousFixedWindowEnd =
        previousFixedWindow.timestamp() + windowLengthMillis;
    // Weight of the previous window based on overlap with the sliding window.
    double previousFixedWindowWeight =
        Math.max(0, previousFixedWindowEnd - slidingWindowStart)
            / (double) windowLengthMillis;

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

  private record SlidingWindow(FixedWindow previousFixedWindow,
                               FixedWindow currentFixedWindow) {
  }

  private record FixedWindow(long timestamp, int count) {
  }
}
