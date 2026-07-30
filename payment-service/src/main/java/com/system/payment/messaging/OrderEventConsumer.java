package com.system.payment.messaging;

import com.system.payment.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.system.payment.service.PaymentService;

@Component
@Slf4j
@RequiredArgsConstructor // <-- ADD THIS ANNOTATION HERE
public class OrderEventConsumer {

    private final PaymentService paymentService;

    // This tells Spring Boot: "Bind to this queue. If it, the exchange, or the routing key are missing, create them instantly!"
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order.created.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", type = "topic"),
            key = "order.created.routing.key"
    ))
    public void consumeOrderCreatedEvent(OrderEvent event) {
        log.info("RECEIVED EVENT: Order {} created for User {} with total amount: ${}",
                event.orderId(), event.userId(), event.totalAmount());

        // TODO: We will write the actual payment processing and database logic here next!
        paymentService.processPayment(event);
    }
}