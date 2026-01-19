package com.example.notification.application;

import com.example.notification.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public List<NotificationDto> getNotificationsByRecipient(String recipientId) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientId().equals(recipientId))
                .map(NotificationDto::from)
                .toList();
    }
}
