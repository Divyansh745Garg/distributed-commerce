package com.system.notification.dto;

import java.util.UUID;

public record PaymentEvent(
        UUID orderId,
        String status
) {}
