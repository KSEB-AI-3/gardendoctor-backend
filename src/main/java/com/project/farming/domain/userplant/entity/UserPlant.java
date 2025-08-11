package com.project.farming.domain.userplant.entity;

import com.project.farming.domain.diary.entity.DiaryUserPlant;
import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.image.entity.ImageFile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    private String plantName; // 식물 종류(등록된 식물, 직접 입력)

    @Column(nullable = false, length = 20)
    private String plantNickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm; // 등록된 텃밭
    
    private String plantingPlace; // 심은 장소(등록된 텃밭, 직접 입력)
    private LocalDateTime plantedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // 알림 관련
    @Column(nullable = false)
    private boolean isNotificationEnabled; // 알림 수신 여부

    @Column(nullable = false)
    private int waterIntervalDays; // 물 주는 주기(일 단위)
    
    private LocalDate lastWateredDate; // 마지막 물 준 날짜
    
    @Column(nullable = false)
    private boolean watered; // 물 주기 여부

    @Column(nullable = false)
    private int pruneIntervalDays; // 가지치기 주기(일 단위)

    private LocalDate lastPrunedDate; // 마지막 가지치기 날짜

    @Column(nullable = false)
    private boolean pruned; // 가지치기 여부

    @Column(nullable = false)
    private int fertilizeIntervalDays; // 영양제 주는 주기(일 단위)

    private LocalDate lastFertilizedDate; // 마지막 영양제 준 날짜

    @Column(nullable = false)
    private boolean fertilized; // 영양제 주기 여부

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
        this.plantName = plantName;
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

    public void updateIsNotificationEnabled(boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled;
    }

    public void updateUserPlantIntervalDays(
            int waterIntervalDays, int pruneIntervalDays, int fertilizeIntervalDays) {
        this.waterIntervalDays = waterIntervalDays;
        this.pruneIntervalDays = pruneIntervalDays;
        this.fertilizeIntervalDays = fertilizeIntervalDays;
    }

    public void updateUserPlantStatus(boolean watered, boolean pruned, boolean fertilized) {
        this.watered = watered;
        this.pruned = pruned;
        this.fertilized = fertilized;
    }

    public void updateLastWateredDate(LocalDate lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }

    public void updateLastPrunedDate(LocalDate lastPrunedDate) {
        this.lastPrunedDate = lastPrunedDate;
    }

    public void updateLastFertilizedDate(LocalDate lastFertilizedDate) {
        this.lastFertilizedDate = lastFertilizedDate;
    }

    public void updateUserPlantImage(ImageFile userPlantImageFile) {
        this.userPlantImageFile = userPlantImageFile;
    }

    //UserPlant 삭제시 연결된 DiaryUserPlant, Diary도 삭제되게 처리.
    @OneToMany(mappedBy = "userPlant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<DiaryUserPlant> diaryUserPlants;
}
