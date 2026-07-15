package com.system.notification.messaging;

import com.system.notification.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @RabbitListener(queues = "order.notification.queue")
    public void handleOrderConfirmation(OrderEvent event) {
        log.info("RECEIVED RABBITMQ MESSAGE!");
        log.info("Processing notification for Order ID: {}", event.getOrderId());
        log.info("Simulating sending email to: {} for total amount: ${}", event.getCustomerEmail(), event.getTotalPrice());

        try {
            // Simulating the time it takes to connect to an SMTP server
            Thread.sleep(2000);
            log.info("Email successfully sent to {}", event.getCustomerEmail());
        } catch (InterruptedException e) {
            log.error("Failed to send email", e);
        }
    }
}