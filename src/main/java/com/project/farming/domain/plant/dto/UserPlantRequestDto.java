package com.project.farming.domain.plant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserPlantRequestDto {
    private Long userId;
    private String plantName;

    @NotBlank(message = "식물의 이름을 입력해주세요.")
    @Size(max = 20, message = "식물 이름은 최대 20글자까지 입력할 수 있습니다.")
    private String nickname;

    private String plantingPlace;
    private LocalDateTime plantedDate;
    private String notes;
    private String imageUrl;
}
