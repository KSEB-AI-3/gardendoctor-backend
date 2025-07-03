// src/main/java/com/project/urbanfarm/controller/dto/TokenRefreshRequestDto.java
package com.project.farming.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRefreshRequestDto {
    @NotBlank(message = "리프레시 토큰은 필수 입력 값입니다.")
    private String refreshToken;
}