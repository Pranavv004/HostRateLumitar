package com.example.HostRateLumiter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitingService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Integer> customLimits;

    public RateLimitingService() {
        // Define custom rate limits per IP (requests per minute)
        customLimits = new ConcurrentHashMap<>();
        customLimits.put("192.168.1.1", 3); // Example IP: 3 requests/minute
        customLimits.put("192.168.1.101", 5); // Example IP: 5 requests/minute
        // Add more IPs as needed
    }

    public boolean allowRequest(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(String clientIp) {
        // Default: 5 requests/minute for unlisted IPs
        int limit = customLimits.getOrDefault(clientIp, 5);
        System.out.println("IP: " + clientIp + ", Limit: " + limit);
        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}