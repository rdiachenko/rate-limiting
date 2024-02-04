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

    // If there is a new user, initialize an empty sliding window for them.
    // Otherwise, get an existing window which contains current and
    // previous fixed windows with request counts.
    SlidingWindow slidingWindow = userSlidingWindow.computeIfAbsent(userId,
        k -> new SlidingWindow(new FixedWindow(now, 0),
            new FixedWindow(now, 0)));

    FixedWindow currentFixedWindow = slidingWindow.currentFixedWindow();
    FixedWindow previousFixedWindow = slidingWindow.previousFixedWindow();

    // If it is time to start a new fixed window, point previous
    // fixed window to the current one and reset the current window
    // with the request timestamp and zero count.
    if (currentFixedWindow.timestamp() + windowLengthMillis < now) {
      previousFixedWindow = currentFixedWindow;
      currentFixedWindow = new FixedWindow(now, 0);
      userSlidingWindow.put(userId,
          new SlidingWindow(previousFixedWindow, currentFixedWindow));
    }

    // Calculate the weight for the previous fixed window.
    // When previous fixed window is far in the past and is not
    // covered by the current sliding window, then the previous
    // window doesn't have any weight which is equivalent to zero.
    long slidingWindowStart = Math.max(0, now - windowLengthMillis);
    long previousFixedWindowEnd =
        previousFixedWindow.timestamp() + windowLengthMillis;
    double previousFixedWindowWeight =
        Math.max(0, previousFixedWindowEnd - slidingWindowStart)
            / (double) windowLengthMillis;

    // Calculate current request count within the sliding window
    // based on the previous fixed window weight and
    // the number of requests made within the current fixed window.
    int count = (int) (previousFixedWindow.count()
        * previousFixedWindowWeight
        + currentFixedWindow.count());

    // If a number of requests within the sliding window exceeds the limit,
    // disallow this request. Otherwise, update request count in the current
    // fixed window and allow the request.
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
