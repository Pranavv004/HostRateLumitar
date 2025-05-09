package com.example.HostRateLumiter;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class QueryController {
    private final RateLimitingService rateLimitingService;
    private final ClickHouseDataSource dataSource;
    private final AtomicInteger totalRequests = new AtomicInteger();
    private final AtomicInteger discardedRequests = new AtomicInteger();

    public QueryController(RateLimitingService rateLimitingService,
                           @Value("${spring.datasource.url}") String jdbcUrl,
                           @Value("${spring.datasource.username}") String username,
                           @Value("${spring.datasource.password}") String password) throws Exception {
        this.rateLimitingService = rateLimitingService;
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        this.dataSource = new ClickHouseDataSource(jdbcUrl, properties);
    }

    @GetMapping("/query")
    public ResponseEntity<String> executeQuery(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        totalRequests.incrementAndGet();
        if (rateLimitingService.allowRequest(clientIp)) {
            try (com.clickhouse.jdbc.ClickHouseConnection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM testdb.countries LIMIT 1")) {
                if (rs.next()) {
                    String country = rs.getString("name");
                    return ResponseEntity.ok(String.format("Result: %s, IP: %s", country, clientIp));
                }
                return ResponseEntity.ok("No results, IP: " + clientIp);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Query failed: " + e.getMessage());
            }
        } else {
            discardedRequests.incrementAndGet();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded");
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<String> getMetrics() {
        return ResponseEntity.ok("Total requests: " + totalRequests.get() + ", Discarded: " + discardedRequests.get());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}