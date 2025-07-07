package com.project.farming.domain.notification.dto;

import com.project.farming.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    // 필드 이름도 엔티티와 일관성 있게 NotificationId로 변경하는 것이 좋습니다.
    // 하지만 현재 ResponseDto에서 `id`로 정의되어 있으므로, getter만 변경하겠습니다.
    private Long id; // DTO 필드명은 `id` 유지
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                // Notification 엔티티의 ID 필드 이름이 'NotificationId'이므로 해당 getter 사용
                .id(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}