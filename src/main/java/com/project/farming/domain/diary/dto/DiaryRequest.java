// src/main/java/com/project/farming/domain/diary/dto/DiaryRequest.java (수정된 예시)
package com.project.farming.domain.diary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가하여 @RequestBody에서 값 주입 가능하게 함

import java.util.List;

@Getter
@Setter // @RequestBody로 받을 때 필요
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일지 생성 및 수정 요청 DTO")
public class DiaryRequest {

    @NotBlank(message = "제목은 필수 입력 사항입니다.")
    @Schema(description = "일지 제목", example = "우리 토마토 첫 열매 맺은 날!")
    private String title;

    @Schema(description = "일지 내용", example = "드디어 토마토에 작은 열매가 보이기 시작했어요. 너무 신기하네요.")
    private String content;

    // --- 기존 imageUrl 필드 제거 ---
    // private String imageUrl;

    @NotNull(message = "물주기 여부는 필수 입력 사항입니다.")
    @Schema(description = "물주기 여부", example = "true")
    private Boolean watered; // boolean 대신 Boolean을 사용하여 null 허용 여부를 명확히 할 수 있음

    @NotNull(message = "가지치기 여부는 필수 입력 사항입니다.")
    @Schema(description = "가지치기 여부", example = "false")
    private Boolean pruned;

    @NotNull(message = "영양제 주기 여부는 필수 입력 사항입니다.")
    @Schema(description = "영양제 주기 여부", example = "true")
    private Boolean fertilized;

    @Schema(description = "일지에 연결할 사용자 식물(UserPlant) ID 목록", example = "[1, 3, 5]")
    private List<Long> selectedUserPlantIds;

    @Schema(description = "기존 이미지 삭제 여부 (일지 수정 시 사용)", example = "false")
    private boolean deleteExistingImage; // 일지 수정 시 기존 이미지 삭제 여부
}