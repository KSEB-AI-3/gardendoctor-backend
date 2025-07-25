// src/main/java/com/project/farming/domain/notification/service/NotificationService.java
package com.project.farming.domain.notification.service;

import com.project.farming.domain.fcm.FcmService;
import com.project.farming.domain.notification.dto.NotificationRequestDto;
import com.project.farming.domain.notification.dto.NotificationResponseDto;
import com.project.farming.domain.notification.entity.Notification;
import com.project.farming.domain.notification.repository.NotificationRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.AccessDeniedException; // 새로 추가된 예외 임포트
import com.project.farming.global.exception.NotificationNotFoundException; // 새로 추가된 예외 임포트
import com.project.farming.global.exception.UserNotFoundException; // UserNotFoundException 유지
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 특정 사용자에게 알림을 생성하고 FCM 푸시 알림을 발송하는 핵심 로직.
     * 스케줄러나 다른 서비스에서 User 엔티티를 직접 넘겨줄 때 사용됩니다.
     */
    @Transactional
    public void createAndSendNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false) // 처음 생성 시 읽지 않음 상태
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
                // TODO: FCM 토큰 무효화 로직 연동 필요 시 여기에 추가
            }
        } else {
            log.warn("⚠️ User with ID {} has no FCM token, skipping push notification.", user.getUserId());
        }
    }

    /**
     * 컨트롤러에서 (User ID와 DTO를 받아) 알림을 생성하고 발송할 때 사용.
     * User 엔티티를 조회한 후 내부 createAndSendNotification 메서드를 호출합니다.
     * (관리자 또는 내부 시스템용)
     */
    @Transactional
    public void createAndSendNotificationFromDto(Long userId, NotificationRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다: " + userId));
        createAndSendNotification(user, requestDto.getTitle(), requestDto.getMessage());
    }

    /**
     * 현재 로그인한 사용자의 알림 목록 조회 (페이징 적용)
     */
    public Page<NotificationResponseDto> getNotificationsForUser(User user, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return notifications.map(NotificationResponseDto::from);
    }

    /**
     * 특정 사용자 ID를 통해 알림 목록 조회 (페이징 없이 모든 알림)
     * 이 메서드는 현재 컨트롤러에서 사용되지 않지만, 다른 곳에서 필요할 수 있어 유지합니다.
     */
    public List<NotificationResponseDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 알림 읽음 처리 (현재 로그인한 사용자의 알림인지 확인)
     */
    @Transactional
    public NotificationResponseDto markNotificationAsRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));

        // 알림의 수신자가 현재 사용자가 맞는지 확인 (권한 체크)
        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("User is not authorized to access this notification.");
        }

        notification.markAsRead(); // 읽음 처리
        return NotificationResponseDto.from(notification);
    }

    /**
     * [내부 시스템 전용] 특정 알림 ID로 읽음 처리 (권한 체크 없음)
     * 이 메서드는 일반 사용자 API에서 직접 호출되지 않아야 합니다.
     * 예를 들어, 배치 작업이나 관리자 도구에서 특정 알림을 강제로 읽음 처리할 때 사용될 수 있습니다.
     */
    @Transactional
    public void markAsReadInternal(Long notificationId) { // 메서드명 변경하여 내부용임을 명확히
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));
        notification.markAsRead();
    }

    /**
     * 현재 로그인한 사용자의 읽지 않은 알림 개수 조회
     */
    public long countUnreadNotifications(User currentUser) {
        return notificationRepository.countByUserAndIsReadFalse(currentUser);
    }

    /**
     * 특정 알림 삭제 (현재 로그인한 사용자 본인의 알림만 가능)
     */
    @Transactional
    public void deleteNotification(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));

        // 알림의 수신자가 현재 사용자가 맞는지 확인 (권한 체크)
        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("User is not authorized to delete this notification.");
        }

        notificationRepository.delete(notification);
    }

    /**
     * 현재 로그인한 사용자의 모든 알림 삭제
     * 이 메서드는 호출하는 곳에서 사용자 ID와 현재 로그인한 사용자가 일치하는지 확인합니다.
     */
    @Transactional
    public void deleteAllUserNotifications(Long userId, User currentUser) {
        // 알림을 삭제할 userId와 현재 로그인한 사용자의 userId가 일치하는지 확인 (권한 체크)
        if (!userId.equals(currentUser.getUserId())) {
            throw new AccessDeniedException("User is not authorized to delete all notifications for this user ID.");
        }
        notificationRepository.deleteByUserId(userId);
    }

    /**
     * [관리자 전용] 특정 사용자 ID를 가진 모든 알림을 삭제 (권한 체크 없음)
     * 이 메서드는 관리자 기능 또는 내부 시스템에서만 사용되어야 합니다.
     */
    @Transactional
    public void deleteAllUserNotificationsInternal(Long userId) { // 메서드명 변경하여 내부용임을 명확히
        // 관리자용이므로 사용자 존재 여부만 확인하거나, 호출하는 쪽에서 보장한다고 가정
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        notificationRepository.deleteByUserId(userId);
    }
}