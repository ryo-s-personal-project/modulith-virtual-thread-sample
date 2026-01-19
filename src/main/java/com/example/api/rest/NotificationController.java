package com.example.api.rest;

import com.example.notification.application.NotificationDto;
import com.example.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping("/{recipientId}")
    public ResponseEntity<List<NotificationDto>> getNotifications(@PathVariable String recipientId) {
        List<NotificationDto> notifications = notificationService.getNotificationsByRecipient(recipientId);
        return ResponseEntity.ok(notifications);
    }
}
