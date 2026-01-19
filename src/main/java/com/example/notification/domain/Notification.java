package com.example.notification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String recipientId;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    
    private LocalDateTime sentAt;
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }
    
    // ドメインロジック
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }
    
    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }
    
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
