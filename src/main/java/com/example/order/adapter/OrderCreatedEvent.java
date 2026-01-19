package com.example.order.adapter;

import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    String customerId,
    String productId,
    Integer quantity,
    java.math.BigDecimal totalAmount
) {
}
