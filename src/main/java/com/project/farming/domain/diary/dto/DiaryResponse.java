package com.project.farming.domain.diary.dto;

import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.entity.DiaryUserPlant;
import com.project.farming.domain.plant.entity.UserPlant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponse {
    private Long diaryId;
    private Long userId; // 일지 작성자 ID
    private String title;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean watered;
    private Boolean pruned;
    private Boolean fertilized;
    private List<UserPlantInfo> userPlants; // 연결된 UserPlant 정보 (ID와 닉네임)

    public DiaryResponse(Diary diary) {
        this.diaryId = diary.getDiaryId();
        this.userId = diary.getUser().getUserId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        this.imageUrl = diary.getImageUrl();
        this.createdAt = diary.getCreatedAt();
        this.updatedAt = diary.getUpdatedAt();
        this.watered = diary.isWatered();
        this.pruned = diary.isPruned();
        this.fertilized = diary.isFertilized();
        // DiaryUserPlant를 UserPlantInfo DTO로 변환하여 리스트에 담음
        this.userPlants = diary.getDiaryUserPlants().stream()
                .map(DiaryUserPlant::getUserPlant) // DiaryUserPlant에서 UserPlant를 가져옴
                .map(UserPlantInfo::new) // UserPlant를 UserPlantInfo DTO로 변환
                .collect(Collectors.toList());
    }

    // 내부 DTO: 연결된 UserPlant의 ID와 닉네임만 전달
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPlantInfo {
        private Long userPlantId;
        private String nickname;

        public UserPlantInfo(UserPlant userPlant) {
            this.userPlantId = userPlant.getUserPlantId();
            this.nickname = userPlant.getNickname();
        }
    }
}