package com.example.inventory.application;

import com.example.inventory.domain.InventoryItem;

import java.util.UUID;

public record InventoryDto(
    UUID id,
    String productId,
    Integer availableQuantity,
    Integer reservedQuantity,
    Integer totalQuantity
) {
    public static InventoryDto from(InventoryItem item) {
        return new InventoryDto(
                item.getId(),
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getTotalQuantity()
        );
    }
}
