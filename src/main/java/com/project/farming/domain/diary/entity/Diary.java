package com.project.farming.domain.diary.entity;

import com.project.farming.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diaries", indexes = @Index(name = "idx_user_diary", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean watered;

    @Column(nullable = false)
    private boolean pruned;

    @Column(nullable = false)
    private boolean fertilized;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryUserPlant> diaryUserPlants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 일지 내용을 업데이트하는 비즈니스 메서드
    public void updateDiary(String title, String content, String imageUrl,
                            boolean watered, boolean pruned, boolean fertilized) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.watered = watered;
        this.pruned = pruned;
        this.fertilized = fertilized;
    }

    // DiaryUserPlant를 추가하는 비즈니스 메서드 (양방향 관계 설정 포함)
    public void addDiaryUserPlant(DiaryUserPlant diaryUserPlant) {
        diaryUserPlants.add(diaryUserPlant);
        // 양방향 관계의 주인이 아닌 쪽에서도 관계를 설정해 줘야 영속성 컨텍스트가 제대로 동작
        diaryUserPlant.setDiary(this);
    }

    // 모든 DiaryUserPlant 연결을 해제하고 컬렉션을 비우는 비즈니스 메서드
    public void clearDiaryUserPlants() {
        // 기존 연결을 끊음 (orphanRemoval=true 설정으로 DB에서 삭제도 처리)
        diaryUserPlants.forEach(diaryUserPlant -> diaryUserPlant.setDiary(null));
        diaryUserPlants.clear();
    }
}