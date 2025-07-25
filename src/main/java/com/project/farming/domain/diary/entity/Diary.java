package com.project.farming.domain.diary.entity;

import com.project.farming.domain.user.entity.User;
import com.project.farming.global.image.entity.ImageFile;
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

    /** Diary의 대표 이미지
     * Diary는 하나의 이미지만 가집니다.
     * cascade = CascadeType.ALL은 Diary가 저장/삭제될 때 연관된 ImageFile도 함께 처리할지 결정합니다.
    여기서는 ImageFile을 다른 도메인에서도 재사용할 수 있으므로,
    Diary 삭제 시 ImageFile이 자동으로 삭제되지 않도록 CascadeType을 명시하지 않거나 MERGE/PERSIST만 고려합니다.
    그러나 보통 대표 이미지는 해당 도메인에 종속적이므로, DELETE 시 ImageFile도 삭제하는 것이 일반적입니다.
    여기서는 Diary에 종속적인 이미지라고 가정하고 CascadeType.ALL을 사용합니다.
     */

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "dairy_image_file_id") // image_files 테이블의 image_file_id를 참조
    private ImageFile diaryImageFile; // 일지에 연결된 ImageFile 객체


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
    // imageUrl 대신 ImageFile 객체를 받도록 변경
    public void updateDiary(String title, String content, ImageFile diaryImage,
                            boolean watered, boolean pruned, boolean fertilized) {
        this.title = title;
        this.content = content;
        this.setDiaryImage(diaryImage); // 이미지 업데이트 로직은 별도 메서드로 분리하는 것이 좋습니다.
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

    // 일지 이미지 설정/업데이트를 위한 비즈니스 메서드
    public void setDiaryImage(ImageFile imageFile) {
        this.diaryImageFile = imageFile;
        // ImageFile은 domainType과 domainId로 주인을 관리하므로,
        // 이 곳에서 ImageFile의 역방향 관계를 명시적으로 설정할 필요는 없습니다.
        // ImageFileService에서 ImageFile을 저장할 때 domainType과 domainId를 설정합니다.
    }
}