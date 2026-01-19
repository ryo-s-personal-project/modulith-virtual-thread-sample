package com.example.inventory.adapter;

import java.util.UUID;

public record InventoryReservedEvent(
    UUID orderId,
    String productId,
    Integer quantity
) {
}
