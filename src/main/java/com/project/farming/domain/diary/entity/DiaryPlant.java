package com.project.farming.domain.diary.entity;

import com.project.farming.domain.plant.entity.Plant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diary_plant", indexes = {
        @Index(name = "idx_diary_plant_diary", columnList = "diary_id"),
        @Index(name = "idx_diary_plant_plant", columnList = "plant_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiaryPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryPlantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;
}
