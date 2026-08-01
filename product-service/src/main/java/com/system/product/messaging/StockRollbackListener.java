package com.system.product.messaging;

import com.system.product.dto.StockRollbackEvent;
import com.system.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockRollbackListener {

    private final ProductService productService;

    // THIS IS THE FIX: Auto-declaring the queue and exchange!
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "stock.rollback.queue", durable = "true"),
            exchange = @Exchange(value = "stock.exchange", type = "topic"),
            key = "stock.rollback.key"
    ))
    public void handleStockRollback(StockRollbackEvent event) {
        log.error("🚨 Payment Failed! Catching Rollback Event for Product ID: {}", event.getProductId());

        // Execute the compensating transaction
        productService.restoreStock(event.getProductId(), event.getQuantity());
    }
}