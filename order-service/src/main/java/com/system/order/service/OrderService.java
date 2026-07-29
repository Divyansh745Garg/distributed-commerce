package com.system.order.service;

import com.system.order.dto.*;
import com.system.order.model.Order;
import com.system.order.model.OrderItem;
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

    // 1. RabbitMQ Template injected here!
    private final RabbitTemplate rabbitTemplate;

    @Value("${application.config.product-url}")
    private String productUrl;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Placing order for user: {}", request.userId());

        // 2. Initialize the parent Order
        Order order = Order.builder()
                .userId(request.userId())
                .status("PENDING")
                .build();

        // 3. Process each item in the cart via RestTemplate
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

        // 4. Calculate the Total Amount
        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // 5. Save to Database
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved successfully with ID: {}", savedOrder.getId());

        // 6. BROADCAST THE EVENT TO RABBITMQ
        OrderEvent event = new OrderEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                event
        );
        log.info("OrderCreated event published to RabbitMQ for order: {}", savedOrder.getId());

        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(item.getId(), item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(), order.getTotalAmount(), itemResponses);
    }
}