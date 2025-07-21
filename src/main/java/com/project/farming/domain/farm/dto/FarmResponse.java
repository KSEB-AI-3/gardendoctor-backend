package com.project.farming.domain.farm.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class FarmResponse {
    private Long farmId;
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
    private String imageUrl;
}
