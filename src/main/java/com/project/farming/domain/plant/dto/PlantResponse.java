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
    private String species;
    private String season;
    private String plantImageUrl;
}
