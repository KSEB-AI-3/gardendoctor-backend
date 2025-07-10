package com.project.farming.domain.plant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlantRequestDto {

    @NotBlank(message = "식물의 이름을 입력해주세요.")
    private String name;

    private String englishName;
    private String species;
    private String season;
    private String imageUrl;
}
