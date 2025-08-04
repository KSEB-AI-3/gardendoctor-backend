package com.project.farming.domain.user.dto;

import com.project.farming.domain.user.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String oauthProvider;
    private String role;
    private String subscriptionStatus;

    // 엔티티 -> DTO 변환 메서드
    public static UserMeResponseDto from(User user) {
        return UserMeResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                // 지연 로딩을 안전하게 처리하여 URL만 추출
                .profileImageUrl(
                        user.getProfileImageFile() != null
                                ? user.getProfileImageFile().getImageUrl()
                                : null
                )
                .oauthProvider(user.getOauthProvider())
                .role(user.getRole().toString())
                .subscriptionStatus(user.getSubscriptionStatus())
                .build();
    }
}