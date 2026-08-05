package com.system.order.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String aggregateId; // The Order ID

    private String eventType; // e.g., "ORDER_CREATED"

    @Column(columnDefinition = "TEXT")
    private String payload; // The JSON representation of the event

    private String status; // PENDING, COMPLETED, FAILED

    private LocalDateTime createdAt;
}