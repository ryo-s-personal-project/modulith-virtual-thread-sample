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
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getRecipientId() {
        return recipientId;
    }
    
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public NotificationStatus getStatus() {
        return status;
    }
    
    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
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
