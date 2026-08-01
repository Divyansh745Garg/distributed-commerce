package com.system.order.model;

public enum OrderStatus {
    CREATED,           // 1. Order saved to DB, starting Saga
    STOCK_RESERVED,    // 2. Product Service successfully deducted inventory
    PAYMENT_PENDING,   // 3. Waiting for Payment Service
    COMPLETED,         // 4. Payment Success (Saga Complete)
    PAYMENT_FAILED,    // 5A. Payment Failed (Triggering Rollback)
    CANCELLED          // 5B. Rollback Complete, Order dead
}