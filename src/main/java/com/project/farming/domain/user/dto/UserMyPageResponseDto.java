package com.project.farming.domain.user.dto;

import com.project.farming.domain.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserMyPageResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String oauthProvider;
    private UserRole role;
    private String subscriptionStatus;
}
