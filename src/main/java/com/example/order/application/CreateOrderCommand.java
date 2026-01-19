package com.example.order.application;

import java.math.BigDecimal;

public record CreateOrderCommand(
    String customerId,
    String productId,
    Integer quantity,
    BigDecimal unitPrice
) {
}
