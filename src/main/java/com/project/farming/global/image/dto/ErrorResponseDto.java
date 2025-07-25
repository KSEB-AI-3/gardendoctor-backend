package com.project.farming.global.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "에러 응답 DTO")
public class ErrorResponseDto {
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
    private String message;

    @Schema(description = "에러 코드", example = "BAD_REQUEST")
    private String errorCode;
}
