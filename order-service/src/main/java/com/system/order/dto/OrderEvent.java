package com.system.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderEvent(
        UUID orderId,
        String userId,
        BigDecimal totalAmount,
        String status
) {}