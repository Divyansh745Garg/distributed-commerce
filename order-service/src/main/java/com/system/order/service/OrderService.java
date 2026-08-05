package com.system.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.order.dto.*;
import com.system.order.model.Order;
import com.system.order.model.OrderItem;
import com.system.order.model.OrderStatus;
import com.system.order.model.OutboxEvent;
import com.system.order.repository.OrderRepository;
import com.system.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    // Inject ObjectMapper and OutboxEventRepository
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${application.config.product-url}")
    private String productUrl;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Starting Saga for user: {}", request.userId());

        // 1. STATE: CREATED
        Order order = Order.builder()
                .userId(request.userId())
                .status(OrderStatus.CREATED)
                .build();

        // 2. Synchronous Check via RestTemplate
        List<OrderItem> orderItems = request.items().stream().map(itemRequest -> {

            log.info("Fetching product details for ID: {}", itemRequest.productId());
            ProductResponse product = restTemplate.getForObject(
                    productUrl + "/" + itemRequest.productId(),
                    ProductResponse.class
            );

            if (product == null || !product.isActive()) {
                throw new IllegalArgumentException("Product not found or inactive: " + itemRequest.productId());
            }
            if (product.stockQuantity() < itemRequest.quantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.name());
            }

            return OrderItem.builder()
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.quantity())
                    .price(product.price())
                    .order(order)
                    .build();
        }).collect(Collectors.toList());

        order.setOrderItems(orderItems);

        // 3. STATE: STOCK_RESERVED
        order.setStatus(OrderStatus.STOCK_RESERVED);

        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 4. Save Order to DB
        Order savedOrder = orderRepository.save(order);

        // 5. STATE: PAYMENT_PENDING
        savedOrder.setStatus(OrderStatus.PAYMENT_PENDING);
        savedOrder = orderRepository.save(savedOrder);
        log.info("Order {} transitioned to {}", savedOrder.getId(), savedOrder.getStatus());

        // 6. SAVE EVENT TO OUTBOX (Transactional Outbox Pattern)
        OrderEvent event = new OrderEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus().name()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(savedOrder.getId().toString())
                    .eventType("ORDER_CREATED")
                    .payload(eventJson)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Saved OrderCreatedEvent to Outbox for Order ID: {}", savedOrder.getId());

        } catch (Exception e) {
            // If JSON serialization fails, the whole @Transactional method rolls back!
            throw new RuntimeException("Failed to serialize Outbox event", e);
        }

        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(item.getId(), item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus().name(), order.getTotalAmount(), itemResponses);
    }
}