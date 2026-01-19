package com.example.shipping.application;

import com.example.shipping.domain.Shipment;
import com.example.shipping.infrastructure.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {
    
    private final ShipmentRepository shipmentRepository;
    
    @Transactional
    public ShipmentDto createShipment(UUID orderId, String customerId) {
        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(Shipment.ShipmentStatus.PENDING)
                .build();
        
        shipment = shipmentRepository.save(shipment);
        log.info("配送情報を作成しました: 配送ID={}", shipment.getId());
        return ShipmentDto.from(shipment);
    }
    
    @Transactional
    public ShipmentDto processShipping(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + shipmentId));
        
        shipment.ship();
        shipment = shipmentRepository.save(shipment);
        
        log.info("配送を処理しました: 配送ID={}", shipment.getId());
        return ShipmentDto.from(shipment);
    }
    
    public ShipmentDto getShipment(UUID orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order: " + orderId));
        return ShipmentDto.from(shipment);
    }
}
