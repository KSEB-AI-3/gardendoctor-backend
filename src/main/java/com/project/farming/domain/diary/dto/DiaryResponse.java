// src/main/java/com/project/farming/domain/diary/dto/DiaryResponse.java (수정된 예시)
package com.project.farming.domain.diary.dto;

import com.project.farming.domain.diary.entity.Diary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Lombok Setter 추가 (선택 사항)

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter // 필요에 따라 추가
@NoArgsConstructor
@Schema(description = "일지 응답 DTO")
public class DiaryResponse {
    @Schema(description = "일지 ID", example = "1")
    private Long diaryId;

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;

    @Schema(description = "일지 제목", example = "우리 토마토 첫 열매 맺은 날!")
    private String title;

    @Schema(description = "일지 내용", example = "드디어 토마토에 작은 열매가 보이기 시작했어요. 너무 신기하네요.")
    private String content;

    @Schema(description = "이미지 URL", example = "https://your-s3-bucket/diary/image123.jpg")
    private String imageUrl; // ImageFile의 imageUrl을 직접 노출

    @Schema(description = "물주기 여부", example = "true")
    private boolean watered;

    @Schema(description = "가지치기 여부", example = "false")
    private boolean pruned;

    @Schema(description = "영양제 주기 여부", example = "true")
    private boolean fertilized;

    @Schema(description = "생성일", example = "2024-07-25T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일", example = "2024-07-25T11:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "연결된 사용자 식물 ID 목록", example = "[1, 3, 5]")
    private List<Long> connectedUserPlantIds;

    public DiaryResponse(Diary diary) {
        this.diaryId = diary.getDiaryId();
        this.userId = diary.getUser().getUserId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        // ImageFile 객체에서 imageUrl을 가져오도록 변경
        this.imageUrl = (diary.getDiaryImageFile() != null) ? diary.getDiaryImageFile().getImageUrl() : null;
        this.watered = diary.isWatered();
        this.pruned = diary.isPruned();
        this.fertilized = diary.isFertilized();
        this.createdAt = diary.getCreatedAt();
        this.updatedAt = diary.getUpdatedAt();
        this.connectedUserPlantIds = diary.getDiaryUserPlants().stream()
                .map(dup -> dup.getUserPlant().getUserPlantId())
                .collect(Collectors.toList());
    }
}