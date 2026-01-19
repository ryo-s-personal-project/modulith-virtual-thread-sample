package com.example.shipping.adapter;

import com.example.shipping.application.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingEventListener {
    
    private final ShippingService shippingService;
    private final ApplicationEventPublisher eventPublisher;
    
    @ApplicationModuleListener
    @Async
    public void onOrderConfirmed(com.example.order.adapter.OrderConfirmedEvent event) {
        log.info("OrderConfirmedEventを受信しました: 注文ID={} (スレッド: {})", 
                event.orderId(), Thread.currentThread());
        
        // Simulate I/O operation (e.g., calling shipping API)
        simulateShippingApiCall();
        
        var shipmentDto = shippingService.createShipment(event.orderId(), event.customerId());
        var processedShipment = shippingService.processShipping(shipmentDto.id());
        
        ShipmentCreatedEvent shipmentEvent = new ShipmentCreatedEvent(
                processedShipment.id(),
                processedShipment.orderId(),
                processedShipment.customerId(),
                processedShipment.trackingNumber()
        );
        
        eventPublisher.publishEvent(shipmentEvent);
        log.info("配送情報を作成しました: 注文ID={}, 追跡番号={}", 
                event.orderId(), processedShipment.trackingNumber());
    }
    
    private void simulateShippingApiCall() {
        try {
            // Simulate external API call delay
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
