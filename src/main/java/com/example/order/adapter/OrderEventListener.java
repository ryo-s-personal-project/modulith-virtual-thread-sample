package com.example.order.adapter;

import com.example.order.application.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final OrderService orderService;
    
    @ApplicationModuleListener
    @Async
    public void onInventoryReserved(com.example.inventory.adapter.InventoryReservedEvent event) {
        log.info("InventoryReservedEventを受信しました: 注文ID={} (スレッド: {})", 
                event.orderId(), Thread.currentThread());
        
        orderService.confirmOrder(event.orderId());
    }
    
    @ApplicationModuleListener
    @Async
    public void onInventoryReservationFailed(com.example.inventory.adapter.InventoryReservationFailedEvent event) {
        log.info("InventoryReservationFailedEventを受信しました: 注文ID={} (スレッド: {})", 
                event.orderId(), Thread.currentThread());
        
        orderService.cancelOrder(event.orderId());
    }
}
