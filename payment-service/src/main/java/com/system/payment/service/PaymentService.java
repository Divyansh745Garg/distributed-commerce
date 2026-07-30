package com.system.payment.service;

import com.system.payment.dto.OrderEvent;
import com.system.payment.model.Payment;
import com.system.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.system.payment.config.RabbitMQConfig;
import com.system.payment.dto.PaymentEvent;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate; // 1. Inject the template
    @Transactional
    public void processPayment(OrderEvent event) {
        log.info("Processing payment of ${} for Order {}", event.totalAmount(), event.orderId());

        // 1. Simulate a call to a Payment Gateway (Stripe/Razorpay)
        String mockTransactionId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 2. Build the Payment Record
        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .userId(event.userId())
                .amount(event.totalAmount())
                .status("SUCCESS") // Simulating a successful charge
                .transactionId(mockTransactionId)
                .build();

        // 3. Save to Database
        paymentRepository.save(payment);
        log.info("Payment SUCCESS. Transaction ID: {} saved to database.", mockTransactionId);

        // TODO: In the next step, we will broadcast a "PaymentCompleted" event back to RabbitMQ!
        // 2. BROADCAST THE SUCCESS EVENT!
        PaymentEvent paymentEvent = new PaymentEvent(event.orderId(), "COMPLETED");
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                paymentEvent
        );
        log.info("PaymentCompleted event published for order: {}", event.orderId());
    }
}
