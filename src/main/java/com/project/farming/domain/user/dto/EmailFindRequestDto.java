package com.project.farming.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailFindRequestDto {
    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    private String nickname;
}