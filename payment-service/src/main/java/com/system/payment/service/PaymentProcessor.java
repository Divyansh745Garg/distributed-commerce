package com.system.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "payment:idempotency:";

    public boolean processPayment(String idempotencyKey, double amount) {
        String redisKey = KEY_PREFIX + idempotencyKey;

        // Atomically set the key ONLY if it doesn't exist. Lock expires in 24 hours.
        Boolean isUniqueRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isUniqueRequest)) {
            log.warn("Duplicate payment request blocked for key: {}", idempotencyKey);
            throw new IllegalStateException("Duplicate transaction detected. Please wait or check your order status.");
        }

        try {
            log.info("Contacting Mock Stripe API to charge ${}...", amount);
            // Simulate network delay to a 3rd party payment gateway
            Thread.sleep(1000);

            // Mark as successful
            redisTemplate.opsForValue().set(redisKey, "SUCCESS", Duration.ofHours(24));
            log.info("Payment successful for key: {}", idempotencyKey);
            return true;

        } catch (Exception e) {
            // If the payment system crashes, release the lock so the user can try again
            redisTemplate.delete(redisKey);
            log.error("Payment failed", e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }
}
