package com.project.farming.domain.plant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserPlantRequest {
    
    @NotNull(message = "사용자 정보를 입력해주세요.")
    private Long userId;

    @NotBlank(message = "식물 종류를 입력해주세요.")
    private String plantName;

    @NotBlank(message = "식물의 별명을 입력해주세요.")
    @Size(max = 20, message = "식물의 별명은 최대 20글자까지 입력할 수 있습니다.")
    private String nickname;

    @NotNull(message = "식물을 심은 장소를 입력해주세요.")
    private Integer gardenUniqueId;

    private String plantingPlace;
    private LocalDateTime plantedDate;
    private String notes;
    private String imageUrl;
}
