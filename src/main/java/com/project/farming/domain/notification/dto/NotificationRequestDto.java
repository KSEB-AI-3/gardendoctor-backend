package com.project.farming.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

//FCM 연동 및 테스트용 수동 생성 시 사용:
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "알림 생성 요청 DTO (단일 또는 다중 사용자)")
public class NotificationRequestDto {

    @NotEmpty(message = "알림을 받을 사용자 ID는 필수 입력 사항입니다.")
    @Schema(description = "알림을 받을 사용자 ID 목록", example = "[1, 2, 3]")
    private List<Long> userIds; // 이제 이것만 사용

    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 100, message = "알림 제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "알림 내용은 필수입니다.")
    @Size(max = 500, message = "알림 내용은 500자를 초과할 수 없습니다.")
    private String message;
}