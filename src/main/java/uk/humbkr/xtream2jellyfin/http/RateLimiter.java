package uk.humbkr.xtream2jellyfin.http;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RateLimiter {

    private final int maxRequests;

    private final long windowDurationMillis;

    private long windowStart;

    private int requestCount;

    public RateLimiter(String rateLimit) {
        if (rateLimit == null || rateLimit.isBlank()) {
            rateLimit = "100/s";
        }

        String[] parts = rateLimit.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rate limit format: " + rateLimit + ". Expected format: '{count}/{unit}' (e.g., '100/s', '2/m')");
        }

        try {
            this.maxRequests = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid request count in rate limit: " + parts[0], e);
        }

        String unit = parts[1].trim().toLowerCase();
        this.windowDurationMillis = switch (unit) {
            case "s" -> 1000L;           // 1 second
            case "m" -> 60_000L;         // 1 minute
            case "h" -> 3_600_000L;      // 1 hour
            case "d" -> 86_400_000L;     // 1 day
            default ->
                    throw new IllegalArgumentException("Invalid time unit: " + unit + ". Supported units: s, m, h, d");
        };

        this.windowStart = System.currentTimeMillis();
        this.requestCount = 0;

        log.debug("Rate limiter initialized: {} requests per {} ms", maxRequests, windowDurationMillis);
    }

    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        long windowEnd = windowStart + windowDurationMillis;

        // Check if we're in a new window
        if (now >= windowEnd) {
            // Reset for new window
            windowStart = now;
            requestCount = 0;
        }

        // Check if we've exceeded the limit in current window
        if (requestCount >= maxRequests) {
            // Wait until next window
            long waitTime = windowEnd - now;
            if (waitTime > 0) {
                log.debug("Rate limit reached ({}/{}), waiting {} ms until next window",
                        requestCount, maxRequests, waitTime);
                Thread.sleep(waitTime);
            }
            // Reset for new window
            windowStart = System.currentTimeMillis();
            requestCount = 0;
        }

        // Increment request count
        requestCount++;
    }

}
