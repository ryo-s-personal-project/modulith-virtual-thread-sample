package com.example.notification.adapter;

import com.example.notification.domain.Notification;
import com.example.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final NotificationRepository notificationRepository;
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onOrderCreated(com.example.order.adapter.OrderCreatedEvent event) {
        log.info("通知用のOrderCreatedEventを受信しました (スレッド: {})", Thread.currentThread());
        
        sendNotification(
                event.customerId(),
                "ORDER_CREATED",
                String.format("注文 #%s が作成され、処理中です。", event.orderId())
        );
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onOrderConfirmed(com.example.order.adapter.OrderConfirmedEvent event) {
        log.info("通知用のOrderConfirmedEventを受信しました (スレッド: {})", Thread.currentThread());
        
        sendNotification(
                event.customerId(),
                "ORDER_CONFIRMED",
                String.format("注文 #%s が確定しました。配送の準備を進めています。", event.orderId())
        );
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional
    public void onShipmentCreated(com.example.shipping.adapter.ShipmentCreatedEvent event) {
        log.info("通知用のShipmentCreatedEventを受信しました (スレッド: {})", Thread.currentThread());
        
        sendNotification(
                event.customerId(),
                "SHIPMENT_CREATED",
                String.format("注文 #%s が発送されました！追跡番号: %s", 
                        event.orderId(), event.trackingNumber())
        );
    }
    
    @Async
    private void sendNotification(String recipientId, String type, String message) {
        log.info("通知を送信中: 受信者ID={}, タイプ={} (スレッド: {})", 
                recipientId, type, Thread.currentThread());
        
        // Simulate I/O operation (e.g., sending email, SMS, push notification)
        simulateNotificationSending();
        
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .message(message)
                .status(Notification.NotificationStatus.PENDING)
                .build();
        
        notification.markAsSent();
        notificationRepository.save(notification);
        log.info("通知を送信しました: 通知ID={}", notification.getId());
    }
    
    private void simulateNotificationSending() {
        try {
            // Simulate external notification service call (email/SMS API)
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
