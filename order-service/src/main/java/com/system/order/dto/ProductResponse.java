package com.system.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

// This helps us deserialize the JSON from the Product Service
public record ProductResponse(
        UUID id,
        String name,
        BigDecimal price,
        Integer stockQuantity,
        boolean isActive
) {}
