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

  /**
   * Constructs a leaky bucket rate limiter.
   *
   * @param capacity       The maximum number of requests a user can make in a given period before being limited.
   * @param period         The time frame in which requests are considered for limiting.
   * @param leaksPerPeriod The number of requests that are allowed to leak out (processed) per period.
   * @param clock          The clock used to determine the current time, facilitating testing with fixed clocks.
   */
  public LeakyBucketRateLimiter(int capacity, Duration period, int leaksPerPeriod, Clock clock) {
    this.capacity = capacity;
    this.period = period;
    this.leaksPerPeriod = leaksPerPeriod;
    this.clock = clock;
  }

  /**
   * Determines if a request by a given user ID is allowed under the current rate limiting rules.
   *
   * @param userId The user ID making the request.
   * @return true if the request is allowed, false otherwise.
   */
  public boolean allowed(String userId) {
    LeakyBucket bucket = userLeakyBucket.computeIfAbsent(userId,
        k -> new LeakyBucket(clock.millis(), 0));

    bucket.leak();

    return bucket.processed();
  }

  private class LeakyBucket {
    private long leakTimestamp; // Timestamp of the last leak.
    private long waterLevel; // Current water level represents the number of pending requests.

    /**
     * Constructs a leaky bucket instance.
     *
     * @param leakTimestamp The initial timestamp of the last leak.
     * @param waterLevel    The initial water level of the bucket.
     */
    LeakyBucket(long leakTimestamp, long waterLevel) {
      this.leakTimestamp = leakTimestamp;
      this.waterLevel = waterLevel;
    }

    /**
     * Simulates the leaking of requests over time. This method adjusts the water level
     * based on the elapsed time since the last leak, applying the defined leak rate.
     */
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

    /**
     * Attempts to process a request by incrementing the water level if under capacity.
     *
     * @return true if the request is processed (under capacity), false if the bucket is full.
     */
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
