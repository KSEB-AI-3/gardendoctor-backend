package com.project.farming.domain.notification.entity;

import com.project.farming.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = @Index(name = "idx_user_notification", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long NotificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  //알림을 받는 사용자

    @Column(nullable = false)
    private String title; //알림 제목

    @Column(columnDefinition = "TEXT")
    private String message; //알림 제목

    @Column(nullable = false)
    private boolean isRead; //읽음 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; //알림 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false; // 생성 시 기본값
    }
    @Builder
    public Notification(User user, String title, String message, boolean isRead) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
    }

    // 알림 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }
}
