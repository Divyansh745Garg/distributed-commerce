package com.system.order.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId; // Storing Auth Service's User ID

    @Column(nullable = false)
    private String status; // e.g., PENDING, CONFIRMED, FAILED

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // The "1 to Many" relationship.
    // CascadeType.ALL means if we save/delete the Order, it automatically saves/deletes the OrderItems!
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
}