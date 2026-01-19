package com.example.shipping.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID orderId;
    
    @Column(nullable = false)
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;
    
    private String trackingNumber;
    
    private LocalDateTime shippedAt;
    
    private LocalDateTime deliveredAt;
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ShipmentStatus.PENDING;
        }
        if (trackingNumber == null) {
            trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
    
    // ドメインロジック
    public void ship() {
        if (this.status != ShipmentStatus.PENDING) {
            throw new IllegalStateException("Shipment can only be shipped from PENDING status");
        }
        this.status = ShipmentStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }
    
    public void markAsDelivered() {
        if (this.status != ShipmentStatus.SHIPPED && this.status != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException("Shipment must be shipped before delivery");
        }
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    public enum ShipmentStatus {
        PENDING,
        SHIPPED,
        IN_TRANSIT,
        DELIVERED
    }
}
