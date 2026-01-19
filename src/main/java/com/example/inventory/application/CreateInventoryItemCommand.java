package com.example.inventory.application;

public record CreateInventoryItemCommand(
    String productId,
    Integer initialQuantity
) {
}
