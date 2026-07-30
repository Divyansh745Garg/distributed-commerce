package com.system.payment.dto; // (Change to com.system.order.dto for the Order Service)

import java.util.UUID;

public record PaymentEvent(
        UUID orderId,
        String status
) {}