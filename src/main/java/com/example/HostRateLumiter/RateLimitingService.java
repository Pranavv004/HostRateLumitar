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
        customLimits = new ConcurrentHashMap<>();
        customLimits.put("117.193.77.254", 2); 
        customLimits.put("192.168.1.1", 4);
        customLimits.put("192.168.1.101", 6);
    }

    public boolean allowRequest(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(String clientIp) {
        int limit = customLimits.getOrDefault(clientIp, 5);
        System.out.println("IP: " + clientIp + ", Limit: " + limit);
        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}