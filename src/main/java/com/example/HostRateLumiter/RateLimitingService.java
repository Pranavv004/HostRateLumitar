package com.example.HostRateLumiter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class RateLimitingService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Semaphore semaphore = new Semaphore(4); // Limit to 4 concurrent queries

    public boolean allowRequest(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createNewBucket);
        try {
            semaphore.acquire(); // Limit concurrent access
            return bucket.tryConsume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            semaphore.release();
        }
    }

    private Bucket createNewBucket(String clientIp) {
        Bandwidth limit = Bandwidth.simple(4, Duration.ofMinutes(1));
        return Bucket.builder().addLimit(limit).build();
    }
}