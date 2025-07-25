package com.project.farming.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
//FCM 연동 및 테스트용 수동 생성 시 사용:
@Getter
@NoArgsConstructor
public class NotificationRequestDto {
    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 100, message = "알림 제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "알림 내용은 필수입니다.")
    @Size(max = 500, message = "알림 내용은 500자를 초과할 수 없습니다.")
    private String message;
}