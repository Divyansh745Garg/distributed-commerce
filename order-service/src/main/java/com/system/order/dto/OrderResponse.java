package com.system.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String userId,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items
        // ^ The generic is the OrderItemResponse DTO
) {}