package com.project.farming.domain.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Firebase Admin SDK 기반 FCM 메시지 발송 구현체
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    @Override
    public void sendMessageTo(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isBlank()) {
            log.warn("⚠️ FCM target token is empty. Skipping push notification.");
            return;
        }

        // 사용자에게 표시될 알림 생성
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // 메시지 구성
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(notification)
                // 필요 시 추가 데이터 전달 가능
                // .putData("key", "value")
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Successfully sent FCM message to [{}]: {}", maskToken(targetToken), response);
        } catch (FirebaseMessagingException e) {
            log.error("🔥 FCM send failed [{}]: {}", maskToken(targetToken), e.getMessage());
            if ("UNREGISTERED".equals(e.getErrorCode()) || "INVALID_ARGUMENT".equals(e.getErrorCode())) {
                log.warn("   -> Token is invalid/unregistered, consider removing from user record.");
                // TODO: DB에서 user의 fcmToken 삭제 로직 연동
            }
        } catch (Exception e) {
            log.error("🔥 Unexpected FCM error [{}]", maskToken(targetToken), e);
        }
    }

    /**
     * FCM 토큰 마스킹 처리
     */
    private String maskToken(String token) {
        if (token.length() < 10) return token;
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
