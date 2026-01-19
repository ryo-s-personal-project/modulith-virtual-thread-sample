package com.example.shipping.adapter;

import java.util.UUID;

public record ShipmentCreatedEvent(
    UUID shipmentId,
    UUID orderId,
    String customerId,
    String trackingNumber
) {
}
