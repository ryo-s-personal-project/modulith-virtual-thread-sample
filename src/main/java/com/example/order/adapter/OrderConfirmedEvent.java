package com.example.order.adapter;

import java.util.UUID;

public record OrderConfirmedEvent(
    UUID orderId,
    String customerId,
    String productId,
    Integer quantity
) {
}
