package com.system.product.service;

import com.system.product.dto.ProductRequest;
import com.system.product.dto.ProductResponse;
import com.system.product.model.Product;
import com.system.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product: {}", request.name());

        // 1. Map DTO to Entity
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .isActive(true)
                .build();

        // 2. Save to Postgres
        Product savedProduct = productRepository.save(product);

        // 3. Map Entity back to Response DTO
        return mapToResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List getAllActiveProducts() {
        log.info("Fetching all active products");
        return productRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        log.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        return mapToResponse(product);
    }

    // Helper method to keep our code DRY (Don't Repeat Yourself)
    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive()
        );
    }

    @Transactional
    public void restoreStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for rollback: " + productId));

        // The Compensation: Add the stock back!
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        log.warn("🔄 SAGA COMPENSATION: Restored {} units for product {}", quantity, productId);
    }
}