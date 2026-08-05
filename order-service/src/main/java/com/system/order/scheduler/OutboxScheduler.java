package com.system.order.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.order.dto.OrderEvent;
import com.system.order.model.OutboxEvent;
import com.system.order.repository.OutboxEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // 1. Setup the ASYNC ACK LISTENER
    @PostConstruct
    public void setupCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            // FIXED: Simplified to satisfy IntelliJ
            if (correlationData != null) {
                UUID eventId = UUID.fromString(correlationData.getId());
                if (ack) {
                    // RabbitMQ confirmed it has the message safely on disk!
                    log.info("✅ Async ACK received from RabbitMQ for Event ID: {}", eventId);

                    outboxRepository.findById(eventId).ifPresent(event -> {
                        event.setStatus("COMPLETED");
                        outboxRepository.save(event);
                    });
                } else {
                    // RabbitMQ rejected the message
                    log.error("❌ NACK received from RabbitMQ for Event ID {}. Cause: {}", eventId, cause);
                }
            }
        });
    }

    // Wake up every 5 seconds
    @Scheduled(fixedDelay = 5000)
//    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                log.info("Publishing PENDING event from Outbox: {}", outboxEvent.getId());

                // Decode the JSON back into an Object
                OrderEvent orderEvent = objectMapper.readValue(outboxEvent.getPayload(), OrderEvent.class);

                // Actually publish to RabbitMQ
                rabbitTemplate.convertAndSend("order.exchange", "order.created.routing.key", orderEvent);

                // Mark as completed so it isn't picked up again
                outboxEvent.setStatus("COMPLETED");
                outboxRepository.save(outboxEvent);

            } catch (Exception e) {
                log.error("Failed to publish OutboxEvent ID: {}. Will retry next cycle.", outboxEvent.getId(), e);
                // It remains "PENDING", so the scheduler will try again in 5 seconds!
            }
        }
    }
}