package com.project.farming.domain.fcm;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void sendMessagesTo(List<String> targetTokens, String title, String body) {
        // 최대 500개까지 동시 전송 가능(그 이상은 수정 필요 > 일반 메시지의 Topic)
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .addAllTokens(targetTokens)
                .build();

        BatchResponse response;
        try {
            response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Messages send result - Success: {}, Failure: {}",
                    response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Messages send failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if (response.getFailureCount() > 0) {
            checkFailure(response, targetTokens);
        }
    }

    private void checkFailure(BatchResponse response, List<String> targetTokens) {
        List<SendResponse> responses = response.getResponses();
        List<String> failedTokens = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                failedTokens.add(targetTokens.get(i));
            }
        }
        log.warn("Failed to send messages: {}", failedTokens);
        // TODO: 실패 토큰 DB 저장 또는 재전송 로직 추가
    }

    /**
     * FCM 토큰 마스킹 처리
     */
    private String maskToken(String token) {
        if (token.length() < 10) return token;
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
