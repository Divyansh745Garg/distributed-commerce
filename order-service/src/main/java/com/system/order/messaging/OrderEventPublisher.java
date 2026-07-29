package com.system.order.messaging;

import com.system.order.config.RabbitMQConfig;
import com.system.order.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreatedEvent(OrderEvent event) {
        // Fix 1: Use .orderId() instead of .getOrderId() because it is a Record!
        log.info("Publishing OrderCreated event for Order ID: {}", event.orderId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,    // Fix 2: Updated variable name
                RabbitMQConfig.ORDER_ROUTING_KEY, // Fix 3: Updated variable name
                event
        );
    }
}