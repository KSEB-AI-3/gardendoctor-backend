package com.project.farming.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    @Column(nullable = false)
    private String title; // 공지사항 제목

    @Column(nullable = false)
    private String content; // 공지사항 내용

    @Column(nullable = false)
    private boolean isSent; // 알림 발송 여부

    private LocalDateTime sentAt; // 마지막 알림 발송 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNotice(
            String title, String content, LocalDateTime sentAt) {
        this.title = title;
        this.content = content;
        this.sentAt = sentAt;
    }

    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }
}
