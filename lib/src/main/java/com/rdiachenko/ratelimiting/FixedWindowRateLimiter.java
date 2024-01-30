package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class FixedWindowRateLimiter {

  private final int maxCount;
  private final long windowLengthMillis;
  private final Clock clock;
  private final Map<String, FixedWindow> userFixedWindow = new HashMap<>();

  FixedWindowRateLimiter(int maxCount, long windowLengthMillis, Clock clock) {
    this.maxCount = maxCount;
    this.windowLengthMillis = windowLengthMillis;
    this.clock = clock;
  }

  boolean allowed(String userId) {
    long now = clock.millis();
    FixedWindow fixedWindow = userFixedWindow.get(userId);

    // If there is a new user OR it is time to start a new window,
    // initialize a new fixed window with the current request timestamp.
    if (fixedWindow == null
        || fixedWindow.timestamp() + windowLengthMillis < now) {
      fixedWindow = new FixedWindow(now, 0);
    }

    // If a number of requests within the window exceeds the limit,
    // disallow this request. Otherwise, update the current request count
    // and allow the request.
    if (fixedWindow.count() >= maxCount) {
      return false;
    } else {
      FixedWindow updatedWindow = new FixedWindow(fixedWindow.timestamp(),
          fixedWindow.count() + 1);
      userFixedWindow.put(userId, updatedWindow);
      return true;
    }
  }

  private record FixedWindow(long timestamp, int count) {
  }
}
