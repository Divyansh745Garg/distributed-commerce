package com.system.payment.controller;

import com.system.payment.service.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j; // <-- ADD THIS
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j // <-- ADD THIS
public class PaymentController {

    private final PaymentProcessor paymentProcessor;

    @PostMapping("/charge")
    public ResponseEntity<?> chargePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {

        try {
            boolean isSuccessful = paymentProcessor.processPayment(idempotencyKey, request.amount());
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Payment of $" + request.amount() + " processed."));

        } catch (IllegalStateException e) {
            // 409 Conflict - Duplicate Request Detected by Redis
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "REJECTED", "error", e.getMessage()));

        } catch (Exception e) {
            // 500 Internal Server Error - Something else broke
            // WE NEED THIS TO SEE THE REAL IP ADDRESS IT'S HITTING:
            log.error("CRITICAL: Redis connection failed!", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }

    // A simple DTO record to map the incoming JSON body
    public record PaymentRequest(Double amount) {}
}