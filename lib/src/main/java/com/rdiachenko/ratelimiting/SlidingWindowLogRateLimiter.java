package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SlidingWindowLogRateLimiter {

  private final int maxCount;
  private final long windowLengthMillis;
  private final Clock clock;
  private final Map<String, Deque<Long>> userSlidingWindow = new HashMap<>();

  SlidingWindowLogRateLimiter(int maxCount, long windowLengthMillis, Clock clock) {
    this.maxCount = maxCount;
    this.windowLengthMillis = windowLengthMillis;
    this.clock = clock;
  }

  boolean allowed(String userId) {
    long now = clock.millis();

    // Initialize an empty sliding window for new users,
    // or retrieve the existing window.
    Deque<Long> slidingWindow = userSlidingWindow
        .computeIfAbsent(userId, k -> new LinkedList<>());

    // Remove timestamps outside the current sliding window.
    while (!slidingWindow.isEmpty()
        && slidingWindow.getFirst() + windowLengthMillis < now) {
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
