package com.example.notification.adapter;

import com.example.notification.domain.Notification;
import com.example.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final NotificationRepository notificationRepository;
    
    @ApplicationModuleListener
    @Async
    public void onOrderCreated(com.example.order.adapter.OrderCreatedEvent event) {
        log.info("通知用のOrderCreatedEventを受信しました (スレッド: {})", Thread.currentThread());
        
        sendNotification(
                event.customerId(),
                "ORDER_CREATED",
                String.format("Order #%s has been created and is being processed.", event.orderId())
        );
    }
    
    @ApplicationModuleListener
    @Async
    public void onOrderConfirmed(com.example.order.adapter.OrderConfirmedEvent event) {
        log.info("通知用のOrderConfirmedEventを受信しました (スレッド: {})", Thread.currentThread());
        
        sendNotification(
                event.customerId(),
                "ORDER_CONFIRMED",
                String.format("Order #%s has been confirmed. Preparing for shipment.", event.orderId())
        );
    }
    
    @ApplicationModuleListener
    @Async
    public void onShipmentCreated(com.example.shipping.adapter.ShipmentCreatedEvent event) {
        log.info("通知用のShipmentCreatedEventを受信しました (スレッド: {})", Thread.currentThread());
        
        sendNotification(
                event.customerId(),
                "SHIPMENT_CREATED",
                String.format("Order #%s has been shipped! Tracking number: %s", 
                        event.orderId(), event.trackingNumber())
        );
    }
    
    @Async
    private void sendNotification(String recipientId, String type, String message) {
        log.info("通知を送信中: 受信者ID={}, タイプ={} (スレッド: {})", 
                recipientId, type, Thread.currentThread());
        
        // Simulate I/O operation (e.g., sending email, SMS, push notification)
        simulateNotificationSending();
        
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        
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
