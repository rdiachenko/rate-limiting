package com.rdiachenko.ratelimiting;

import java.util.HashMap;
import java.util.Map;

public class FixedWindowRateLimiter {

    private final int maxCount;
    private final long windowLengthMillis;
    private final Map<String, FixedWindow> userFixedWindow = new HashMap<>();

    FixedWindowRateLimiter(int maxCount, long windowLengthMillis) {
        this.maxCount = maxCount;
        this.windowLengthMillis = windowLengthMillis;
    }

    boolean allowed(String userId) {
        long now = System.currentTimeMillis();
        FixedWindow fixedWindow = userFixedWindow.get(userId);

        // If there is a new user OR it is time to start a new window,
        // initialize a new fixed window with the current request timestamp.
        if (fixedWindow == null || fixedWindow.timestamp() + windowLengthMillis < now) {
            fixedWindow = new FixedWindow(now, 0);
        }

        // If a number of requests within the window exceeds the limit, disallow this request.
        // Otherwise, update the current request count and allow the request.
        if (fixedWindow.count() >= maxCount) {
            return false;
        } else {
            userFixedWindow.put(userId, new FixedWindow(fixedWindow.timestamp(), fixedWindow.count() + 1));
            return true;
        }
    }

    private record FixedWindow(long timestamp, int count) {
    }
}
