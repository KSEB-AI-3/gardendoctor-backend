package com.project.farming.domain.diary.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DiaryResponseDto {

    private Long diaryId;
    private String title;
    private String content;
    private String imageUrl;
    private boolean watered;
    private boolean pruned;
    private boolean fertilized;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PlantSummary> plants;

    @Builder
    @Getter
    public static class PlantSummary {
        private Long plantId;
        private String name;
        private String imageUrl;
    }
}
