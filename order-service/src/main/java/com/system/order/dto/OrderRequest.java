//package com.system.order.dto;
//
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//
//import java.math.BigDecimal;
//
//@Data
//public class OrderRequest {
//
//    @NotNull(message = "Product ID is required")
//    private Long productId;
//
//    @Min(value = 1, message = "Quantity must be at least 1")
//    private Integer quantity;
//
//    @NotNull(message = "Total price is required")
//    @Min(value = 0, message = "Price cannot be negative")
//    private BigDecimal totalPrice;
//
//    @NotBlank(message = "Customer email is required")
//    @Email(message = "Email format is invalid")
//    private String customerEmail;
//}
package com.system.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        @NotEmpty(message = "Order must have at least one item")
        List<@Valid OrderItemRequest> items
) {}