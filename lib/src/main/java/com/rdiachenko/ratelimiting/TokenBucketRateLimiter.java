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
  private final RefillStrategy refillStrategy;
  private final Map<String, TokenBucket> userTokenBucket = new HashMap<>();

  /**
   * Constructs a TokenBucketRateLimiter with the specified parameters.
   *
   * @param capacity        The maximum number of tokens that the bucket can hold.
   * @param period          The period over which tokens are replenished.
   * @param tokensPerPeriod The number of tokens added to the bucket each period.
   * @param clock           The clock instance to use for timing.
   * @param refillStrategy  The strategy for refilling the bucket with tokens.
   */
  public TokenBucketRateLimiter(int capacity, Duration period, int tokensPerPeriod,
                                Clock clock, RefillStrategy refillStrategy) {
    this.capacity = capacity;
    this.period = period;
    this.tokensPerPeriod = tokensPerPeriod;
    this.clock = clock;
    this.refillStrategy = refillStrategy;
  }

  /**
   * Determines if a request from the specified user ID is allowed based on the
   * current state of their token bucket.
   *
   * @param userId The ID of the user making the request.
   * @return true if the request is allowed, false otherwise.
   */
  boolean allowed(String userId) {
    // Initialize an empty bucket for new users or retrieve existing one.
    TokenBucket bucket = userTokenBucket.computeIfAbsent(userId,
        k -> new TokenBucket(clock.millis(), tokensPerPeriod));

    // Refill the bucket with available tokens based on
    // elapsed time since last refill.
    bucket.refill();

    // Allow this request if a token was available and consumed,
    // Otherwise, reject the request.
    return bucket.consume();
  }

  private class TokenBucket {
    private long refillTimestamp; // Timestamp of the last refill.
    private long tokenCount; // Current number of tokens in the bucket.

    /**
     * Constructs a TokenBucket with the specified initial state.
     *
     * @param refillTimestamp The timestamp of the last refill.
     * @param tokenCount      The initial number of tokens in the bucket.
     */
    TokenBucket(long refillTimestamp, long tokenCount) {
      this.refillTimestamp = refillTimestamp;
      this.tokenCount = tokenCount;
    }

    /**
     * Refills the token bucket according to the specified refill strategy.
     */
    void refill() {
      switch (refillStrategy) {
        case GREEDY -> refillGreedy();
        case INTERVALLY -> refillIntervally();
        default -> throw new IllegalStateException("Unsupported refill strategy: "
            + refillStrategy);
      }
    }

    /**
     * Regenerates tokens in a greedy manner. This strategy tries to add tokens to the bucket as soon
     * as possible without waiting for the entire period to elapse. For example, a configuration of
     * "2 tokens per 1 second" would add 1 token every 500 milliseconds.
     */
    private void refillGreedy() {
      long now = clock.millis();
      long elapsedTime = now - refillTimestamp;
      long availableTokens = elapsedTime * tokensPerPeriod / period.toMillis();

      tokenCount = Math.min(tokenCount + availableTokens, capacity);
      refillTimestamp += availableTokens * period.toMillis() / tokensPerPeriod;
    }

    /**
     * Regenerates tokens at fixed intervals. Unlike the greedy strategy, this method waits for the
     * entire period to elapse before regenerating the full amount of tokens designated for that period.
     */
    private void refillIntervally() {
      long now = clock.millis();
      long elapsedTime = now - refillTimestamp;
      long elapsedPeriods = elapsedTime / period.toMillis();
      long availableTokens = elapsedPeriods * tokensPerPeriod;

      tokenCount = Math.min(tokenCount + availableTokens, capacity);
      refillTimestamp += elapsedPeriods * period.toMillis();
    }

    /**
     * Consumes a single token from the bucket, if available.
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

  /**
   * Defines the strategies available for refilling tokens in the bucket.
   */
  public enum RefillStrategy {
    GREEDY, INTERVALLY
  }
}
