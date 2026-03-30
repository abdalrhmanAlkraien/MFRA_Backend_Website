package com.mfra.website.module.auth.service;

import com.mfra.website.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    public void checkRateLimit(String ip, String endpoint, int maxRequests, int windowMinutes) {
        String key = RATE_LIMIT_PREFIX + endpoint + ":" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, windowMinutes, TimeUnit.MINUTES);
        }

        if (count != null && count > maxRequests) {
            log.warn("Rate limit exceeded for IP {} on endpoint {}", ip, endpoint);
            throw new RateLimitExceededException("Access temporarily restricted. Try again later.");
        }
    }
}
