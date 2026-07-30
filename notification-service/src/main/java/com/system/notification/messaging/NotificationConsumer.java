package com.system.notification.messaging;

import com.system.notification.dto.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "notification.payment.queue", durable = "true"),
            exchange = @Exchange(value = "payment.exchange", type = "topic"),
            key = "payment.completed.routing.key"
    ))
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("RECEIVED EVENT: Payment {} for Order {}", event.status(), event.orderId());

        if ("COMPLETED".equals(event.status())) {
            log.info("SIMULATING EMAIL SENDED: \n" +
                    "=========================================\n" +
                    "To: Customer\n" +
                    "Subject: Your Order {} is Confirmed!\n" +
                    "Body: We have successfully received your payment. We will ship your items shortly!\n" +
                    "=========================================", event.orderId());
        }
    }
}