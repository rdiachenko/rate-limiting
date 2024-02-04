package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SlidingWindowRateLimiter {

  private final int maxCount;
  private final long windowLengthMillis;
  private final Clock clock;
  private final Map<String, Deque<Long>> userSlidingWindow = new HashMap<>();

  SlidingWindowRateLimiter(int maxCount, long windowLengthMillis, Clock clock) {
    this.maxCount = maxCount;
    this.windowLengthMillis = windowLengthMillis;
    this.clock = clock;
  }

  boolean allowed(String userId) {
    long now = clock.millis();

    // If there is a new user, initialize an empty sliding window for them.
    // Otherwise, get an existing window which contains timestamps of the
    // previously made requests.
    Deque<Long> slidingWindow = userSlidingWindow
        .computeIfAbsent(userId, k -> new LinkedList<>());

    // Remove request timestamps which are outside the current sliding window.
    while (!slidingWindow.isEmpty()
        && slidingWindow.getFirst() + windowLengthMillis < now) {
      slidingWindow.removeFirst();
    }

    // If the current number of requests within the window exceeds the limit,
    // disallow this request. Otherwise, include the current request into
    // the window and allow the request.
    if (slidingWindow.size() >= maxCount) {
      return false;
    } else {
      slidingWindow.addLast(now);
      return true;
    }
  }
}
