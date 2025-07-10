package com.project.farming.domain.farm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "farm_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FarmInfo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long farmId;

    @Column(unique = true, nullable = false)
    private Integer gardenUniqueId; // 텃밭 고유번호

    private String operator; // 운영주체
    private String name;
    private String roadNameAddress; // 도로명주소
    private String lotNumberAddress; // 지번주소
    private String facilities;
    private Boolean available;
    private String contact; // 신청방법
    private Double latitude; // 위도
    private Double longitude; // 경도
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String imageUrl; // 추가하기

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
            Integer gardenUniqueId, String operator, String name,
            String roadNameAddress, String lotNumberAddress,
            String facilities, Boolean available, String contact,
            Double latitude, Double longitude, String imageUrl) {

        this.gardenUniqueId = gardenUniqueId;
        this.operator = operator;
        this.name = name;
        this.roadNameAddress = roadNameAddress;
        this.lotNumberAddress = lotNumberAddress;
        this.facilities = facilities;
        this.available = available;
        this.contact = contact;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }
}
