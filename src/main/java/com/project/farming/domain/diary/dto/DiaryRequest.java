package com.project.farming.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryRequest {
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Content cannot exceed 2000 characters")
    private String content;

    private String imageUrl; // 이미지 URL은 선택 사항

    @NotNull(message = "Watered status cannot be null")
    private Boolean watered;

    @NotNull(message = "Pruned status cannot be null")
    private Boolean pruned;

    @NotNull(message = "Fertilized status cannot be null")
    private Boolean fertilized;

    // 일지에 연결할 UserPlant ID 목록. 클라이언트에서 드롭다운 선택 등으로 넘어옴.
    private List<Long> selectedUserPlantIds;
}