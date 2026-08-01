package com.system.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void verifyIdempotency(String idempotencyKey, Object payload) {
        try {
            // 1. Hash the incoming payload
            String payloadString = objectMapper.writeValueAsString(payload);
            String currentHash = DigestUtils.md5DigestAsHex(payloadString.getBytes());
            String redisKey = "idempotency:order:" + idempotencyKey;

            // 2. Try to acquire the lock using SETNX
            Boolean isNewRequest = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, currentHash, Duration.ofHours(24));

            // 3. If the key already exists, validate the payload hash!
            if (Boolean.FALSE.equals(isNewRequest)) {
                String storedHash = redisTemplate.opsForValue().get(redisKey);

                if (!currentHash.equals(storedHash)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency collision! Same key used with a different payload.");
                } else {
                    throw new ResponseStatusException(HttpStatus.OK,
                            "Request already processed successfully.");
                }
            }
        } catch (Exception e) {
            if (e instanceof ResponseStatusException) throw (ResponseStatusException) e;
            throw new RuntimeException("Failed to verify idempotency", e);
        }
    }
}