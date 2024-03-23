package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class LeakyBucketRateLimiter {

  private final int capacity;
  private final Duration period;
  private final int leaksPerPeriod;
  private final Clock clock;
  private final Map<String, LeakyBucket> userLeakyBucket = new HashMap<>();

  public LeakyBucketRateLimiter(int capacity, Duration period, int leaksPerPeriod, Clock clock) {
    this.capacity = capacity;
    this.period = period;
    this.leaksPerPeriod = leaksPerPeriod;
    this.clock = clock;
  }

  public boolean allowed(String userId) {
    LeakyBucket bucket = userLeakyBucket.computeIfAbsent(userId,
        k -> new LeakyBucket(clock.millis(), 0));

    bucket.leak();

    return bucket.processed();
  }

  private class LeakyBucket {
    private long leakTimestamp; // Timestamp of the last leak.
    private long waterLevel; // Current water level represents the number of pending requests.

    LeakyBucket(long leakTimestamp, long waterLevel) {
      this.leakTimestamp = leakTimestamp;
      this.waterLevel = waterLevel;
    }

    void leak() {
      long now = clock.millis();
      long elapsedTime = now - leakTimestamp;
      long elapsedPeriods = elapsedTime / period.toMillis();
      long leaks = elapsedPeriods * leaksPerPeriod;

      if (leaks > 0) {
        waterLevel = Math.max(0, waterLevel - leaks);
        leakTimestamp = now;
      }
    }

    boolean processed() {
      if (waterLevel < capacity) {
        ++waterLevel;
        return true;
      } else {
        return false;
      }
    }
  }
}
