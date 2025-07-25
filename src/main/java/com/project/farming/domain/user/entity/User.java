package com.project.farming.domain.user.entity;

import com.project.farming.global.image.entity.ImageFile;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"oauthProvider", "oauthId"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(length = 20, nullable = true)
    private String oauthProvider;

    @Column(length = 255, nullable = true)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(length = 512)
    private String fcmToken;

    @Column(length = 20, nullable = false)
    private String subscriptionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_file_id")
    private ImageFile profileImageFile; // ImageFile 엔티티 참조

    public void updateProfileImageFile(ImageFile imageFile) {
        this.profileImageFile = imageFile;
    }

    // --- 업데이트 메서드 ---
    public void updateNickname(String nickname) {
        this.nickname = nickname;
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