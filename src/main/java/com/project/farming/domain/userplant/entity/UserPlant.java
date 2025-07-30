package com.project.farming.domain.userplant.entity;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.image.entity.ImageFile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "user_plants",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "plant_nickname"})
        },
        indexes = @Index(name = "idx_user_plant", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userPlantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant; // 등록된 식물

    private String plantName; // 식물 이름(등록된 식물, 직접 입력)

    @Column(nullable = false, length = 20)
    private String plantNickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm; // 등록된 텃밭
    
    private String plantingPlace; // 심은 장소(등록된 텃밭, 직접 입력)
    private LocalDateTime plantedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_plant_image_file_id", nullable = false)
    private ImageFile userPlantImageFile;

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

    // 사용자 입력 식물이면 식물 이름 수정 가능
    public void updatePlantName(String plantName) {
        if (Objects.equals(this.plant.getPlantName(), "기타")) {
            this.plantName = plantName;
        }
    }

    public void updateUserPlantInfo(String plantNickname, String notes) {
        this.plantNickname = plantNickname;
        this.notes = notes;
    }

    // 식물을 다른 곳에 옮겨 심는 경우
    public void updatePlantingPlace(Farm farm, String plantingPlace) {
        this.farm = farm;
        this.plantingPlace = plantingPlace;
    }

    public void updateUserPlantImage(ImageFile userPlantImageFile) {
        this.userPlantImageFile = userPlantImageFile;
    }
}
