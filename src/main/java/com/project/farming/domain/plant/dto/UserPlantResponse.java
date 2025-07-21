package com.project.farming.domain.plant.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserPlantResponse {
    private Long userPlantId;
    private Long userId;
    private String plantName;
    private String nickname;
    private String plantingPlace;
    private LocalDateTime plantedDate;
    private String notes;
    private String userPlantImageUrl;

    private String plantEnglishName;
    private String species;
    private String season;
    private String plantImageUrl;
}
