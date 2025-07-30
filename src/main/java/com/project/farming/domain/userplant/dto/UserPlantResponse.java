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
    private String plantName;
    private String plantNickname;
    private String plantingPlace;
    private LocalDateTime plantedDate;
    private String notes;
    private String userPlantImageUrl;

    private String plantEnglishName;
    private String species;
    private String season;
    private String plantImageUrl;
}
