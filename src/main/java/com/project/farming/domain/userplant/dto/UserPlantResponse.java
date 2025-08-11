package com.project.farming.domain.userplant.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserPlantResponse {
    private Long userPlantId;
    private String plantName; // 식물 이름(등록된 식물, 직접 입력)
    private String plantNickname;
    private String plantingPlace; // 심은 장소(등록된 텃밭, 직접 입력)
    private LocalDateTime plantedDate;
    private String notes;
    private Boolean watered; // 물 주기 여부
    private Boolean pruned; // 가지치기 여부
    private Boolean fertilized; // 영양제 주기 여부
    private String userPlantImageUrl;

    // 등록된 식물인 경우
    private String plantEnglishName; // 식물 영문 이름
    private String species; // 식물 분류
    private String season; // 계절
    private String plantImageUrl;
}
