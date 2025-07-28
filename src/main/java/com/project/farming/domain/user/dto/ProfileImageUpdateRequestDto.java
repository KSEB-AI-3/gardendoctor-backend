package com.project.farming.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "프로필 이미지 업데이트 요청 DTO")
public class ProfileImageUpdateRequestDto {
    @Schema(description = "새로운 프로필 이미지 파일 ID. 이미지를 삭제하려면 이 필드를 null로 설정", nullable = true)
    private Long profileImageFileId;
}