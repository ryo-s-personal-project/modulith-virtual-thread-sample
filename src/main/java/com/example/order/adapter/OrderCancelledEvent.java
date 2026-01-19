package com.example.order.adapter;

import java.util.UUID;

public record OrderCancelledEvent(
    UUID orderId,
    String productId,
    Integer quantity
) {
}
