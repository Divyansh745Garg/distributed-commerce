package com.system.order.controller;

import com.system.order.dto.OrderRequest;
import com.system.order.dto.OrderResponse;
import com.system.order.service.IdempotencyService;
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
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity placeOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        idempotencyService.verifyIdempotency(idempotencyKey, request);
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}