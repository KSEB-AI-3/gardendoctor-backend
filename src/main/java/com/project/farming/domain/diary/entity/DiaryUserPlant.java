package com.project.farming.domain.diary.entity;

import com.project.farming.domain.userplant.entity.UserPlant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diary_user_plant", indexes = {
        @Index(name = "idx_diary_user_plant_diary", columnList = "diary_id"),
        @Index(name = "idx_diary_user_plant_user_plant", columnList = "user_plant_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiaryUserPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryUserPlantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_plant_id", nullable = false)
    private UserPlant userPlant;

    // 양방향 관계 설정을 위한 메서드: 외부에서 직접 호출하지 않도록 protected로 변경
    protected void setDiary(Diary diary) {
        this.diary = diary;
    }
}