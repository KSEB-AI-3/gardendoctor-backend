package com.project.farming.domain.user.entity;

import com.project.farming.domain.analysis.entity.PhotoAnalysis;
import com.project.farming.domain.chat.entity.Chat;
import com.project.farming.domain.notification.entity.Notification;
import com.project.farming.domain.subscription.entity.Subscription;
import com.project.farming.domain.userplant.entity.UserPlant;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.jwtToken.RefreshToken;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    // 관리자 페이지에서 사용
    public void updateEmail(String email) {
        this.email = email;
    }

    // 관리자 페이지에서 사용
    public void updateRole(UserRole role) {
        this.role = role;
    }

    // 소셜 로그인 연동 시 사용될 setter (CustomOAuth2UserService에서 호출)
    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public void setOauthId(String oauthId) {
        this.oauthId = oauthId;
    }

    // 1. UserPlant 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<UserPlant> userPlants;

    // 2. Notification 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Notification> notifications;

    // 3. Subscription 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Subscription> subscriptions;

    // 4. RefreshToken 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<RefreshToken> refreshTokens;

    // 5. Chat 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Chat> chats;

    // 6. PhotoAnalysis 엔티티와의 연관관계 추가
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PhotoAnalysis> photoAnalyses = new ArrayList<>();
}