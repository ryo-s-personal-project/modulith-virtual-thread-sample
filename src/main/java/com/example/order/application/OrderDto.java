package com.example.order.application;

import com.example.order.domain.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderDto(
    UUID id,
    String customerId,
    String productId,
    Integer quantity,
    BigDecimal totalAmount,
    Order.OrderStatus status,
    LocalDateTime createdAt
) {
    public static OrderDto from(Order order) {
        return new OrderDto(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
