package com.system.order.repository;

import com.system.order.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // The background thread will use this to find events that haven't been sent yet
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
}