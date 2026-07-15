package com.system.order.controller;

import com.system.order.dto.OrderRequest;
import com.system.order.model.Order;
import com.system.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {
        try {
            Order order = orderService.createOrder(request, idempotencyKey);
            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // 409 Conflict - The user tried to submit the same order twice!
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment processing failed");
        }
    }
}