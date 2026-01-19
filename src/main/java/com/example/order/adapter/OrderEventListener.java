package com.example.order.adapter;

import com.example.order.application.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    
    private final OrderService orderService;
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onInventoryReserved(com.example.inventory.adapter.InventoryReservedEvent event) {
        log.info("InventoryReservedEventを受信しました: 注文ID={} (スレッド: {})", 
                event.orderId(), Thread.currentThread());
        
        orderService.confirmOrder(event.orderId());
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onInventoryReservationFailed(com.example.inventory.adapter.InventoryReservationFailedEvent event) {
        log.info("InventoryReservationFailedEventを受信しました: 注文ID={} (スレッド: {})", 
                event.orderId(), Thread.currentThread());
        
        orderService.cancelOrder(event.orderId());
    }
}
