package com.project.farming.domain.diary.entity;

import com.project.farming.domain.user.entity.User;
import com.project.farming.global.image.entity.ImageFile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate; // LocalDate 임포트
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diaries", indexes = @Index(name = "idx_user_diary", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(toBuilder = true) // 기존 Builder 유지 + toBuilder 추가
@AllArgsConstructor(access = AccessLevel.PRIVATE) // toBuilder를 사용하려면 AllArgsConstructor가 필요하며, private로 설정하여 외부에서 직접 사용을 막습니다.
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "dairy_image_file_id")
    private ImageFile diaryImageFile;

    @Column(nullable = false)
    private LocalDate diaryDate; // ✨ 사용자가 선택한 일지 날짜 필드 추가

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean watered;

    @Column(nullable = false)
    private boolean pruned;

    @Column(nullable = false)
    private boolean fertilized;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiaryUserPlant> diaryUserPlants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✨ 일지 내용을 업데이트하는 비즈니스 메서드 수정
    public void updateDiary(String title, String content, LocalDate diaryDate, ImageFile diaryImage,
                            boolean watered, boolean pruned, boolean fertilized) {
        this.title = title;
        this.content = content;
        this.diaryDate = diaryDate; // ✨ diaryDate 업데이트 추가
        this.setDiaryImage(diaryImage);
        this.watered = watered;
        this.pruned = pruned;
        this.fertilized = fertilized;
    }

    public void addDiaryUserPlant(DiaryUserPlant diaryUserPlant) {
        diaryUserPlants.add(diaryUserPlant);
        diaryUserPlant.setDiary(this);
    }

    public void clearDiaryUserPlants() {
        diaryUserPlants.forEach(diaryUserPlant -> diaryUserPlant.setDiary(null));
        diaryUserPlants.clear();
    }

    public void setDiaryImage(ImageFile imageFile) {
        this.diaryImageFile = imageFile;
    }
}