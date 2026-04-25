package com.catalogcollector.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);
    static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";

    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        String redisKey = "rate:" + clientKey;

        try {
            long now = Instant.now().toEpochMilli();
            long windowStart = now - WINDOW_DURATION.toMillis();

            // Remove entries outside the sliding window
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // Count current requests in window
            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count == null) count = 0L;

            if (count >= MAX_REQUESTS_PER_WINDOW) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader(RATE_LIMIT_LIMIT_HEADER,
                        String.valueOf(MAX_REQUESTS_PER_WINDOW));
                response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
                return;
            }

            // Add current request
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
            redisTemplate.expire(redisKey, WINDOW_DURATION.plusSeconds(10));

            long remaining = MAX_REQUESTS_PER_WINDOW - count - 1;
            response.setHeader(RATE_LIMIT_LIMIT_HEADER,
                    String.valueOf(MAX_REQUESTS_PER_WINDOW));
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));

        } catch (Exception e) {
            // If Redis is unavailable, allow the request through
            logger.warn("Rate limiting unavailable: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Use authenticated user ID if available, otherwise fall back to IP
        java.security.Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return "user:" + principal.getName();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
