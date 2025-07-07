package com.project.farming.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiaryRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private String imageUrl;

    private boolean watered;
    private boolean pruned;
    private boolean fertilized;

    private List<Long> plantIds;
}
