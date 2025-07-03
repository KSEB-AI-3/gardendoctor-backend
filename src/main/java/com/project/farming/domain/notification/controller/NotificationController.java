package com.project.farming.domain.notification.controller;

import com.project.farming.domain.notification.dto.NotificationRequestDto;
import com.project.farming.domain.notification.dto.NotificationResponseDto;
import com.project.farming.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 사용자 알림 전체 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponseDto>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    /**
     * 사용자 알림 생성 + FCM 발송
     */
    @PostMapping("/{userId}")
    public ResponseEntity<Void> createNotification(
            @PathVariable Long userId,
            @RequestBody NotificationRequestDto requestDto
    ) {
        notificationService.createNotification(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 단일 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 알림 전체 삭제
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAllUserNotifications(@PathVariable Long userId) {
        notificationService.deleteAllUserNotifications(userId);
        return ResponseEntity.ok().build();
    }
}
