package com.project.farming.domain.farm.entity;

import com.project.farming.global.image.entity.ImageFile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "farm_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long farmId;

    @Column(unique = true, nullable = false)
    private int gardenUniqueId; // 텃밭 고유번호

    private String operator; // 운영주체
    private String farmName; // 텃밭 이름
    private String roadNameAddress; // 도로명 주소

    @Column(nullable = false)
    private String lotNumberAddress; // 지번 주소

    private String facilities; // 부대시설
    private String contact; // 신청 방법
    private Double latitude; // 위도
    private Double longitude; // 경도

    @Column(nullable = false)
    private boolean available; // 운영 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_image_file_id", nullable = false)
    private ImageFile farmImageFile; // 텃밭 사진

    private LocalDate createdAt;
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDate.now();
        if (this.updatedAt == null) this.updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }

    public void updateFarmInfo(
            Integer gardenUniqueId, String operator, String farmName,
            String roadNameAddress, String lotNumberAddress,
            String facilities, String contact,
            Double latitude, Double longitude, Boolean available) {

        this.gardenUniqueId = gardenUniqueId;
        this.operator = operator;
        this.farmName = farmName;
        this.roadNameAddress = roadNameAddress;
        this.lotNumberAddress = lotNumberAddress;
        this.facilities = facilities;
        this.contact = contact;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
    }

    public void updateFarmImage(ImageFile farmImageFile) {
        this.farmImageFile = farmImageFile;
    }
}
