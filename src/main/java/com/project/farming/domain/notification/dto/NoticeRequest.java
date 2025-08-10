package com.project.farming.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class NoticeRequest {

    @NotBlank(message = "공지사항 제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "공지사항 내용을 입력해주세요.")
    private String content;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime sentAt; // 알림 발송 시간
}
