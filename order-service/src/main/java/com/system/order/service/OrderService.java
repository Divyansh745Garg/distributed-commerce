package com.system.order.service;

import com.system.order.dto.OrderEvent;
import com.system.order.dto.OrderRequest;
import com.system.order.messaging.OrderEventPublisher;
import com.system.order.model.Order;
import com.system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j; // <-- 1. ADD THIS IMPORT

@Service
@RequiredArgsConstructor
@Slf4j // <-- 2. ADD THIS ANNOTATION
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher; // <-- INJECTED PUBLISHER

    public Order createOrder(OrderRequest request, String idempotencyKey) {

        // 1. Process Payment in Redis (Idempotency check)
//        paymentProcessor.processPayment(idempotencyKey, request.getTotalPrice().doubleValue());
        // TODO: Communicate with payment-service via WebClient or RabbitMQ to process payment
        log.info("Order placed, waiting for payment processing...");

        // 2. Save Order to PostgreSQL
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(request.getTotalPrice());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setStatus("PAID");

        Order savedOrder = orderRepository.save(order);

        // 3. Fire Asynchronous Event to RabbitMQ
        OrderEvent event = new OrderEvent(
                savedOrder.getId(),
                savedOrder.getCustomerEmail(),
                savedOrder.getTotalPrice()
        );
        eventPublisher.publishOrderConfirmation(event);

        return savedOrder;
    }
}