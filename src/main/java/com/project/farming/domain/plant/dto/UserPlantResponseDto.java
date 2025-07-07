package com.project.farming.domain.plant.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserPlantResponseDto {
    private String userEmail;
    private String plantName;
    private String nickname;
    private String plantingPlace;
    private LocalDateTime plantedDate;
    private String notes;
    private String imageUrl;
    private String message;
}
