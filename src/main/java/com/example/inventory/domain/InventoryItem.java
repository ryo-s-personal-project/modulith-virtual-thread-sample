package com.example.inventory.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String productId;
    
    @Column(nullable = false)
    private Integer availableQuantity;
    
    @Column(nullable = false)
    private Integer reservedQuantity;
    
    // ドメインロジック
    public boolean hasEnoughStock(Integer requestedQuantity) {
        return availableQuantity >= requestedQuantity;
    }
    
    public void reserve(Integer quantity) {
        if (!hasEnoughStock(quantity)) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }
    
    public void release(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved quantity");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }
    
    public Integer getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }
}
