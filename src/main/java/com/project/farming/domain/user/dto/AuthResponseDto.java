// src/main/java/com/project/urbanfarm/controller/dto/AuthResponseDto.java
package com.project.farming.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private String message;
    private String errorCode;
    private Object data;
}