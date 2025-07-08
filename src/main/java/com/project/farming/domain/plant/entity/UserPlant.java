package com.project.farming.domain.plant.entity;

import com.project.farming.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_plants",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "nickname"})
        },
        indexes = @Index(name = "idx_user_plant", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPlant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userPlantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant; // 등록된 식물

    private String plantName; // 식물 이름(등록된 식물, 직접 입력)

    @Column(nullable = false, length = 20)
    private String nickname;

    private String plantingPlace;
    private LocalDateTime plantedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateUserPlant(String nickname, String plantingPlace,
                                String notes, String imageUrl) {
        this.nickname = nickname;
        this.plantingPlace = plantingPlace;
        this.notes = notes;
        this.imageUrl = imageUrl;
    }
}
