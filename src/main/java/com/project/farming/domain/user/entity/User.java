package com.project.farming.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"oauthProvider", "oauthId"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // @Builder를 사용하면 AllArgsConstructor가 필요할 수 있습니다.
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false) // ⭐ 이메일은 모든 사용자에게 필수
    private String email;

    @Column(nullable = false) // ⭐ 비밀번호는 모든 사용자에게 필수 (소셜은 가상 비밀번호)
    private String password;

    @Column(length = 50, nullable = false) // 닉네임 필수
    private String nickname;

    @Column(length = 255)
    private String profileImage;

    @Column(length = 20, nullable = true) // ⭐ 소셜 로그인 아닐 시 null 허용
    private String oauthProvider; // "google", "kakao", "naver" 등

    @Column(length = 255, nullable = true) // ⭐ 소셜 로그인 고유 ID (일반 로그인 시 null)
    private String oauthId;  // 각 소셜 서비스에서 제공하는 고유 ID (String으로 통일)

    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 저장하도록 설정
    @Column(nullable = false)
    private UserRole role; // ⭐ UserRole 필드 추가

    @Column(length = 512)
    private String fcmToken;

    @Column(length = 20, nullable = false) // 구독 상태 필수
    private String subscriptionStatus;

    // --- 업데이트 메서드 ---
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    // 소셜 로그인 연동 시 사용될 setter (CustomOAuth2UserService에서 호출)
    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public void setOauthId(String oauthId) {
        this.oauthId = oauthId;
    }
}