package com.project.farming.global.oauth;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.entity.UserRole;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.image.entity.DefaultImages; // DefaultImages 임포트 확인
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUserService {

    private final UserRepository userRepository;
    private final ImageFileService imageFileService;

    /**
     * OAuth2 로그인 정보를 기반으로 사용자 정보를 저장하거나 업데이트합니다.
     *
     * @param oauthId OAuth 제공자로부터 받은 사용자 고유 ID
     * @param email 사용자 이메일
     * @param nickname 사용자 닉네임
     * @param oauthProvider OAuth 제공자 (google, kakao, naver 등)
     * @return 저장되거나 업데이트된 User 엔티티
     * @throws OAuth2AuthenticationException 이메일 충돌 또는 알 수 없는 오류 발생 시
     */
    @Transactional
    public User saveOrUpdateUserFromOAuth(String oauthId, String email, String nickname, String oauthProvider) {
        Optional<User> optionalUser = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            // 기존 OAuth 사용자는 닉네임만 업데이트하고, 프로필 이미지는 변경하지 않습니다.
            // (기존에 설정된 사용자 지정 프로필 이미지를 유지하기 위함)
            user.updateNickname(nickname);
            log.info("Existing OAuth user updated: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
        } else {
            optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                // 기존 일반 회원 또는 다른 OAuth 제공자 회원과 이메일 연동
                if (user.getOauthProvider() == null || user.getOauthProvider().isEmpty()) {
                    user.setOauthProvider(oauthProvider);
                    user.setOauthId(oauthId);
                    user.updateNickname(nickname);
                    // ⭐ 연동 시에도 프로필 이미지를 기본 이미지로 설정 ⭐
                    setDefaultProfileImage(user);
                    log.info("Existing normal user linked with OAuth: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
                } else if (!user.getOauthProvider().equals(oauthProvider)) {
                    log.error("Email conflict during OAuth login: Email {} already exists with other Provider {}", email, user.getOauthProvider());
                    throw new OAuth2AuthenticationException("이미 존재하는 이메일입니다. 다른 방식으로 로그인해주세요.");
                } else {
                    log.error("Unexpected duplicate OAuth user detected: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
                    throw new OAuth2AuthenticationException("알 수 없는 오류로 이미 등록된 계정입니다.");
                }
            } else {
                // 새로운 사용자 생성
                String generatedPassword = UUID.randomUUID().toString(); // 임시 비밀번호 생성

                user = User.builder()
                        .email(email)
                        .password(generatedPassword)
                        .nickname(nickname)
                        .oauthProvider(oauthProvider)
                        .oauthId(oauthId)
                        .role(UserRole.USER) // 기본 역할 설정
                        .subscriptionStatus("FREE") // 기본 구독 상태 설정
                        .build();
                user = userRepository.save(user); // 먼저 user 저장하여 userId를 얻음 (FK 관계 설정 전)

                // ⭐ 새로운 사용자 생성 시 프로필 이미지를 DefaultImages.DEFAULT_USER_IMAGE로 설정 ⭐
                setDefaultProfileImage(user);
                userRepository.save(user); // User 엔티티에 ImageFile 정보 반영 후 업데이트

                log.info("New OAuth user created: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
            }
        }
        return userRepository.save(user); // 변경사항이 있으면 최종 저장
    }

    /**
     * 사용자의 프로필 이미지를 DefaultImages.DEFAULT_USER_IMAGE에 해당하는 ImageFile로 설정합니다.
     * 이 메서드는 OAuthUserService 내부에서만 사용됩니다.
     * 이 ImageFile은 애플리케이션 시작 시 미리 DB에 저장되어 있어야 합니다.
     *
     * @param user 대상 User 엔티티
     */
    private void setDefaultProfileImage(User user) {
        Optional<ImageFile> defaultImageOptional = imageFileService.getImageFileByS3Key(DefaultImages.DEFAULT_USER_IMAGE);

        if (defaultImageOptional.isPresent()) {
            // 기존 프로필 이미지가 있다면 삭제
            if (user.getProfileImageFile() != null) {
                // 기본 이미지를 삭제하려고 시도하는 경우도 방지하기 위해 ImageFileService.deleteImage의 로직이 중요합니다.
                // ImageFileService.deleteImage는 기본 이미지는 삭제하지 않도록 되어 있어야 합니다.
                imageFileService.deleteImage(user.getProfileImageFile().getImageFileId());
            }
            user.updateProfileImageFile(defaultImageOptional.get());
            log.debug("사용자 {}의 프로필 이미지를 기본 이미지({})로 설정했습니다.", user.getEmail(), DefaultImages.DEFAULT_USER_IMAGE);
        } else {
            log.warn("기본 사용자 프로필 이미지 ({})를 DB에서 찾을 수 없습니다. 사용자 {}에게 프로필 이미지가 설정되지 않습니다.", DefaultImages.DEFAULT_USER_IMAGE, user.getEmail());
            user.updateProfileImageFile(null); // 기본 이미지를 찾을 수 없으면 null로 설정
        }
    }

    // ⭐ 중요: 이 메서드는 더 이상 소셜 로그인에서 사용하지 않으므로 삭제합니다. ⭐
    // private void updateUserProfileImage(User user, String profileImageUrl) {
    //     if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
    //         ImageFile newProfileImageFile = imageFileService.createExternalImageFile(profileImageUrl, ImageDomainType.USER, user.getUserId());
    //         user.updateProfileImageFile(newProfileImageFile);
    //     } else {
    //         if (user.getProfileImageFile() != null) {
    //             imageFileService.deleteImage(user.getProfileImageFile().getImageFileId());
    //         }
    //         user.updateProfileImageFile(null);
    //     }
    // }
}