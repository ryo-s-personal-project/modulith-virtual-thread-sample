package com.example.shipping.application;

import com.example.shipping.domain.Shipment;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentDto(
    UUID id,
    UUID orderId,
    String customerId,
    Shipment.ShipmentStatus status,
    String trackingNumber,
    LocalDateTime shippedAt,
    LocalDateTime deliveredAt
) {
    public static ShipmentDto from(Shipment shipment) {
        return new ShipmentDto(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getCustomerId(),
                shipment.getStatus(),
                shipment.getTrackingNumber(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt()
        );
    }
}
