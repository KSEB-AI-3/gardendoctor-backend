package com.project.farming.domain.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
//FCM 연동 및 테스트용 수동 생성 시 사용:
@Getter
@NoArgsConstructor
public class NotificationRequestDto {
    private String title;
    private String message;
}