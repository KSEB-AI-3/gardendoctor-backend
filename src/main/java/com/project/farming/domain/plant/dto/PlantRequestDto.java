package com.project.farming.domain.plant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlantRequestDto {
    private String name;
    private String englishName;
    private String species;
    private String season;
    private String imageUrl;
}
