package com.system.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long remainingValidityInSeconds) {
        // Store the token as a key with a dummy value, and set the expiration
        redisTemplate.opsForValue().set(token, "blacklisted", Duration.ofSeconds(remainingValidityInSeconds));
        log.info("Token blacklisted successfully. It will be purged from Redis in {} seconds.", remainingValidityInSeconds);
    }
}