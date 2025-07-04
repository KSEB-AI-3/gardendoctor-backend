package com.project.farming.domain.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "farm_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FarmInfo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private int farmId; // 텃밭고유번호

    private String operator; // 운영주체

    @Column(nullable = false)
    private String name;

    private String roadNameAddress; // 도로명주소
    private String lotNumberAddress; // 지번주소

    @Column(columnDefinition = "TEXT")
    private String details;

    private Boolean available;

    private String contact; // 신청방법

    private double latitude; // 위도
    private double longitude; // 경도

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

    public void updateFarmInfo(
            int farmId, String operator, String name, String roadNameAddress, String lotNumberAddress,
            String details, Boolean available, String contact, double latitude, double longitude) {
        this.farmId = farmId;
        this.operator = operator;
        this.name = name;
        this.roadNameAddress = roadNameAddress;
        this.lotNumberAddress = lotNumberAddress;
        this.details = details;
        this.available = available;
        this.contact = contact;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
