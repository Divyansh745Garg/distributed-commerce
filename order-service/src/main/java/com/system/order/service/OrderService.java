package com.system.order.service;

import com.system.order.dto.*;
import com.system.order.model.Order;
import com.system.order.model.OrderItem;
import com.system.order.model.OrderStatus;
import com.system.order.repository.OrderRepository;
import com.system.order.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

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
        // FIXED: Added <OrderItem> to the List
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

        // No more red lines here because orderItems is explicitly List<OrderItem>!
        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 4. Save to DB
        Order savedOrder = orderRepository.save(order);

        // 5. STATE: PAYMENT_PENDING
        savedOrder.setStatus(OrderStatus.PAYMENT_PENDING);
        savedOrder = orderRepository.save(savedOrder);
        log.info("Order {} transitioned to {}", savedOrder.getId(), savedOrder.getStatus());

        // 6. BROADCAST THE EVENT TO RABBITMQ
        OrderEvent event = new OrderEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus().name()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                event
        );
        log.info("Saga handed off to Payment Service for order: {}", savedOrder.getId());

        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        // FIXED: Added <OrderItemResponse> to the List
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(item.getId(), item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus().name(), order.getTotalAmount(), itemResponses);
    }
}