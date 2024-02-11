package com.rdiachenko.ratelimiting;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TokenBucketRateLimiter {

  private final int capacity;
  private final Duration period;
  private final int tokensPerPeriod;
  private final Clock clock;
  private final Map<String, TokenBucket> userTokenBucket = new HashMap<>();

  public TokenBucketRateLimiter(int capacity, Duration period, int tokensPerPeriod, Clock clock) {
    this.capacity = capacity;
    this.period = period;
    this.tokensPerPeriod = tokensPerPeriod;
    this.clock = clock;
  }

  boolean allowed(String userId) {
    // Initialize an empty bucket for new users or retrieve existing one.
    TokenBucket bucket = userTokenBucket.computeIfAbsent(userId,
        k -> new TokenBucket(clock.millis(), capacity));

    // Refill the bucket with available tokens based on
    // elapsed time since last refill.
    bucket.refill();

    // Allow this request if a token was available and consumed,
    // Otherwise, reject the request.
    return bucket.consume();
  }

  private class TokenBucket {
    private long refillTimestamp;
    private long tokenCount;

    TokenBucket(long refillTimestamp, long tokenCount) {
      this.refillTimestamp = refillTimestamp;
      this.tokenCount = tokenCount;
    }

    /**
     * Refills tokens based on the elapsed time since the last refill.
     * Ensures the token count does not exceed the bucket's capacity.
     */
    void refill() {
      long now = clock.millis();
      long elapsedTime = now - refillTimestamp;
      long availableTokens = elapsedTime * tokensPerPeriod / period.toMillis();

      if (availableTokens > 0) {
        tokenCount = Math.min(tokenCount + availableTokens, capacity);
        refillTimestamp = now;
      }
    }

    /**
     * Consumes a single token if available.
     *
     * @return true if a token was consumed, false otherwise.
     */
    boolean consume() {
      if (tokenCount > 0) {
        --tokenCount;
        return true;
      } else {
        return false;
      }
    }
  }
}
