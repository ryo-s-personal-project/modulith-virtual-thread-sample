package com.example.notification.application;

import com.example.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    String recipientId,
    String type,
    String message,
    Notification.NotificationStatus status,
    LocalDateTime sentAt
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getRecipientId(),
                notification.getType(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getSentAt()
        );
    }
}
