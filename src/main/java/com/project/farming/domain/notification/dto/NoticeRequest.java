package com.project.farming.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class NoticeRequest {

    @NotBlank(message = "알림 제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "알림 내용을 입력해주세요.")
    private String content;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime sentAt; // 전송 시간
}
