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
    private List<DiaryPlant> diaryPlants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDiary(String title, String content, String imageUrl,
                            boolean watered, boolean pruned, boolean fertilized) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.watered = watered;
        this.pruned = pruned;
        this.fertilized = fertilized;
    }

    public void addDiaryPlant(DiaryPlant diaryPlant) {
        diaryPlants.add(diaryPlant);
    }

    public void clearDiaryPlants() {
        diaryPlants.clear();
    }
}
