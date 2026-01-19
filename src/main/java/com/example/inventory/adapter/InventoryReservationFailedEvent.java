package com.example.inventory.adapter;

import java.util.UUID;

public record InventoryReservationFailedEvent(
    UUID orderId,
    String productId,
    Integer quantity,
    String reason
) {
}
