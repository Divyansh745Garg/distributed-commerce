package com.system.order.messaging;

import com.system.order.dto.PaymentEvent;
import com.system.order.model.Order;
import com.system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    @Transactional
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "payment.completed.queue", durable = "true"),
            exchange = @Exchange(value = "payment.exchange", type = "topic"),
            key = "payment.completed.routing.key"
    ))
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("RECEIVED EVENT: Payment {} for Order {}", event.status(), event.orderId());

        // 1. Find the pending order
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.orderId()));

        // 2. Update the status and save!
        order.setStatus(event.status());
        orderRepository.save(order);

        log.info("Order {} status successfully updated to {}", order.getId(), order.getStatus());
    }
}