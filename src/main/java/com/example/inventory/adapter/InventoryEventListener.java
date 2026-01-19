package com.example.inventory.adapter;

import com.example.inventory.domain.InventoryItem;
import com.example.inventory.infrastructure.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {
    
    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onOrderCreated(com.example.order.adapter.OrderCreatedEvent event) {
        log.info("OrderCreatedEventを受信しました: 商品ID={}, 数量={} (スレッド: {})", 
                event.productId(), event.quantity(), Thread.currentThread());
        
        // Simulate I/O operation (e.g., database query, external service call)
        simulateIoOperation();
        
        InventoryItem item = inventoryRepository.findByProductId(event.productId())
                .orElseGet(() -> {
                    log.warn("在庫に商品が見つかりませんでした: 商品ID={}, 新規エントリを作成します", event.productId());
                    return InventoryItem.builder()
                            .productId(event.productId())
                            .availableQuantity(0)
                            .reservedQuantity(0)
                            .build();
                });
        
        if (item.hasEnoughStock(event.quantity())) {
            item.reserve(event.quantity());
            inventoryRepository.save(item);
            
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    event.orderId(),
                    event.productId(),
                    event.quantity()
            );
            
            eventPublisher.publishEvent(reservedEvent);
            log.info("在庫を確保しました: 注文ID={}", event.orderId());
        } else {
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    event.orderId(),
                    event.productId(),
                    event.quantity(),
                    "在庫不足. 利用可能: " + item.getAvailableQuantity() + ", 要求: " + event.quantity()
            );
            
            eventPublisher.publishEvent(failedEvent);
            log.warn("在庫確保に失敗しました: 注文ID={}", event.orderId());
        }
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onOrderCancelled(com.example.order.adapter.OrderCancelledEvent event) {
        log.info("OrderCancelledEventを受信しました: 注文ID={} (スレッド: {})", 
                event.orderId(), Thread.currentThread());
        
        InventoryItem item = inventoryRepository.findByProductId(event.productId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + event.productId()));
        
        item.release(event.quantity());
        inventoryRepository.save(item);
        
        log.info("キャンセルされた注文の在庫を解放しました: 注文ID={}", event.orderId());
    }
    
    private void simulateIoOperation() {
        try {
            // Simulate network I/O or database query delay
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
