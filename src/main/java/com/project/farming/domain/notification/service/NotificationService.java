package com.project.farming.domain.notification.service;

import com.project.farming.domain.fcm.FcmService;
import com.project.farming.domain.notification.dto.NotificationRequestDto;
import com.project.farming.domain.notification.dto.NotificationResponseDto;
import com.project.farming.domain.notification.entity.Notification;
import com.project.farming.domain.notification.repository.NotificationRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.UserNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final FcmService fcmService;


    /**
     * 매일 오전 10시 모든 사용자에게
     * "오늘의 할 일" 알림 전송
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendNotifications() {
        List<String> targetTokens = userRepository.findAll().stream()
                .map(User::getFcmToken)
                .collect(Collectors.toList());
        if (targetTokens.isEmpty()) {
            throw new UserNotFoundException("사용자가 존재하지 않습니다.");
        }
        fcmService.sendMessagesTo(
                targetTokens,
                "\uD83C\uDF31 오늘의 식물 관리 알림",
                "\uD83D\uDCA7 오늘 물 주기와 ✂\uFE0F 가지치기, \uD83D\uDC8A 영양제 주기를 잊지 말고 챙겨주세요.");
    }

    /**
     * 사용자 알림 전체 조회 (최신순)
     */
    public List<NotificationResponseDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
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
     * 사용자에게 알림 생성 + FCM 발송 (외부에서 호출되는 주 진입점)
     */
    @Transactional // 이 메서드에서 트랜잭션을 관리
    public void createAndSendNotification(Long userId, NotificationRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 분리된 전용 메서드를 호출하여 알림 생성 및 발송
        // isRead는 requestDto에 없으므로, 기본값 false로 설정합니다.
        sendNotificationInternal(user, requestDto.getTitle(), requestDto.getMessage(), false);
    }

    /**
     * 특정 사용자에게 알림을 생성하고 발송합니다.
     * (내부적으로 호출되는 범용 메서드)
     * 이 메서드는 이미 User 객체를 가지고 있을 때 사용됩니다.
     */
    @Transactional // 이 메서드는 createAndSendNotification 또는 다른 트랜잭션 내에서 호출될 수 있습니다.
    // 만약 이 메서드만 단독으로 호출될 가능성이 있다면 @Transactional이 필요하지만,
    // 현재 구조상으로는 createAndSendNotification 내에서 호출되므로 생략 가능합니다.
    // 명시적으로 두는 것이 안전할 수 있습니다.
    public void sendNotification(User user, String title, String message) {
        // 기존의 sendNotification 로직에서 isRead 기본값을 추가합니다.
        sendNotificationInternal(user, title, message, false);
    }


    /**
     * 알림을 데이터베이스에 저장하고, 사용자에게 FCM 푸시 알림을 발송하는 내부 로직.
     * 이 메서드는 다른 public 메서드들이 중복 없이 호출하도록 돕습니다.
     * @param user 알림을 받을 사용자 엔티티
     * @param title 알림 제목
     * @param message 알림 내용
     * @param isRead 알림 초기 읽음 상태 (현재는 모두 false로 고정될 수 있으나, 유연성을 위해 파라미터로 추가)
     */
    private void sendNotificationInternal(User user, String title, String message, boolean isRead) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(isRead) // 파라미터로 받은 isRead 값 사용
                .build();

        notificationRepository.save(notification);

        // FCM 발송
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            try {
                fcmService.sendMessageTo(
                        user.getFcmToken(),
                        title,
                        message
                );
            } catch (Exception e) {
                log.error("🔥 Failed to send FCM notification to userId {}: {}", user.getUserId(), e.getMessage(), e);
                // TODO: 필요하다면 무효 토큰 삭제 로직 연동
                // user.clearFcmToken(); // 예시: FcmToken이 유효하지 않을 때 토큰을 지우는 로직
            }
        } else {
            log.warn("⚠️ User with ID {} has no FCM token, skipping push notification.", user.getUserId());
        }
    }
}