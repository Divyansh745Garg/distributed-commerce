package com.system.order.messaging;

import com.system.order.dto.PaymentFailedEvent;
import com.system.order.dto.StockRollbackEvent;
import com.system.order.model.Order;
import com.system.order.model.OrderStatus;
import com.system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedListener {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    // THIS IS THE FIX: Auto-declaring the queue and exchange!
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "payment.failed.queue", durable = "true"),
            exchange = @Exchange(value = "payment.exchange", type = "topic"),
            key = "payment.failed.routing.key"
    ))
    public void handlePaymentFailure(PaymentFailedEvent event) {
        log.error("Received Payment Failure for Order: {}. Triggering Saga Rollback.", event.getOrderId());

        // 1. Find the order and update the State Machine
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        // 2. Loop through the cart and tell the Product Service to restore the stock!
        order.getOrderItems().forEach(item -> {
            StockRollbackEvent rollbackEvent = new StockRollbackEvent(
                    item.getProductId(),
                    item.getQuantity()
            );

            rabbitTemplate.convertAndSend(
                    "stock.exchange",
                    "stock.rollback.key",
                    rollbackEvent
            );

            log.info("Sent rollback command for Product ID: {}", item.getProductId());
        });

        // 3. Finally, mark the order as fully CANCELLED now that rollback is triggered
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}