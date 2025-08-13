package com.project.farming.domain.plant.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlantResponse {
    private Long plantId;
    private String plantName;
    private String plantEnglishName;
    private String species; // 식물 분류
    private String season;
    private String plantImageUrl;
}
