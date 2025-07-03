package com.project.farming.domain.notification.service;

import com.project.farming.domain.fcm.FcmService;
import com.project.farming.domain.notification.dto.NotificationRequestDto;
import com.project.farming.domain.notification.dto.NotificationResponseDto;
import com.project.farming.domain.notification.entity.Notification;
import com.project.farming.domain.notification.repository.NotificationRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService; // ✅ FCM 발송 연동

    /**
     * 사용자 알림 전체 조회 (최신순)
     */
    public List<NotificationResponseDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.markAsRead();
    }

    /**
     * 사용자 알림 전체 삭제
     */
    @Transactional
    public void deleteAllUserNotifications(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }

    /**
     * 사용자에게 알림 생성 + FCM 발송
     */
    @Transactional
    public void createNotification(Long userId, NotificationRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .title(requestDto.getTitle())
                .message(requestDto.getMessage())
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // ✅ FCM 발송
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            try {
                fcmService.sendMessageTo(
                        user.getFcmToken(),
                        requestDto.getTitle(),
                        requestDto.getMessage()
                );
            } catch (Exception e) {
                log.error("🔥 Failed to send FCM notification to userId {}: {}", userId, e.getMessage(), e);
                // TODO: 필요하다면 무효 토큰 삭제 로직 연동
                // user.clearFcmToken();
            }
        } else {
            log.warn("⚠️ User with ID {} has no FCM token, skipping push notification.", userId);
        }
    }
}
