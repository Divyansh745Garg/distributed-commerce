package com.system.product.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Cannot be null in the DB
    @Column(nullable = false)
    private String name;

    // Extends the default varchar(255) to 1000 characters
    @Column(length = 1000)
    private String description;

    // Precision 10, Scale 2 means max value is 99,999,999.99
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    // Here is your Soft Delete implementation!
    // We give it a default value so new products are active by default.
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;
}