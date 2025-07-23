package com.project.farming.domain.user.dto;

import lombok.Getter;

@Getter
public class UserMyPageUpdateRequestDto {
    private String nickname;
    private Long profileImageFileId;
    private Boolean deleteProfileImage; //프로필 이미지 삭제 여부를 명시
    private String fcmToken;
    private String subscriptionStatus;
}
