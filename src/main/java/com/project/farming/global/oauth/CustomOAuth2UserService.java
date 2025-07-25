package com.project.farming.global.oauth;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.entity.UserRole;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.jwtToken.CustomUserDetails;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final ImageFileService imageFileService;
    private final ImageFileRepository imageFileRepository; // 이 리포지토리는 직접 사용하지 않을 수도 있습니다.

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("OAuth2 User Attributes: {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String oauthId;
        String email;
        String nickname = null;
        String profileImageUrl = null;

        if ("google".equals(registrationId)) {
            oauthId = oAuth2User.getName();
            email = oAuth2User.getAttribute("email");
            nickname = oAuth2User.getAttribute("name");
            profileImageUrl = oAuth2User.getAttribute("picture");
        } else if ("kakao".equals(registrationId)) {
            Long kakaoId = oAuth2User.getAttribute("id");
            oauthId = String.valueOf(kakaoId);

            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                Boolean hasEmail = (Boolean) kakaoAccount.get("has_email");
                Boolean emailNeedsAgreement = (Boolean) kakaoAccount.get("email_needs_agreement");

                if (hasEmail != null && hasEmail && (emailNeedsAgreement == null || !emailNeedsAgreement)) {
                    email = (String) kakaoAccount.get("email");
                } else {
                    email = null;
                }

                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                    profileImageUrl = (String) profile.get("profile_image_url");
                }
            } else {
                email = null;
            }
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = oAuth2User.getAttribute("response");
            if (response != null) {
                oauthId = (String) response.get("id");
                email = (String) response.get("email");
                nickname = (String) response.get("nickname");
                profileImageUrl = (String) response.get("profile_image");
            } else {
                log.warn("Naver OAuth2 response attributes are missing.");
                oauthId = null;
                email = null;
            }
        } else {
            log.warn("Unsupported OAuth2 registrationId: {}", registrationId);
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다.");
        }

        if (email == null || email.trim().isEmpty()) {
            log.error("OAuth2 Login Failed: Email not provided by {} for oauthId {}", registrationId, oauthId);
            throw new OAuth2AuthenticationException("이메일 정보는 필수입니다. 이메일 제공에 동의해주세요.");
        }

        User user = saveOrUpdateOAuthUser(oauthId, email, nickname, profileImageUrl, registrationId);

        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }

    @Transactional
    private User saveOrUpdateOAuthUser(String oauthId, String email, String nickname, String profileImageUrl, String oauthProvider) {
        Optional<User> optionalUser;

        optionalUser = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);

        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user.updateNickname(nickname);
            // 기존 사용자 프로필 이미지 업데이트 (ImageFile 처리)
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                // ⭐ 변경된 부분: createExternalImageFile 호출
                ImageFile newProfileImageFile = imageFileService.createExternalImageFile(profileImageUrl, ImageDomainType.USER, user.getUserId());
                user.updateProfileImageFile(newProfileImageFile);
            } else {
                // 기존 프로필 이미지 파일이 있다면 삭제 처리 (선택 사항)
                if (user.getProfileImageFile() != null) {
                    imageFileService.deleteImage(user.getProfileImageFile().getImageFileId());
                }
                user.updateProfileImageFile(null); // 프로필 이미지 URL이 없으면 삭제
            }
            log.info("Existing OAuth user updated: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
        } else {
            optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                if (user.getOauthProvider() == null || user.getOauthProvider().isEmpty()) {
                    user.setOauthProvider(oauthProvider);
                    user.setOauthId(oauthId);
                    user.updateNickname(nickname);
                    // 기존 일반 회원 프로필 이미지 업데이트 (ImageFile 처리)
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        // ⭐ 변경된 부분: createExternalImageFile 호출
                        ImageFile newProfileImageFile = imageFileService.createExternalImageFile(profileImageUrl, ImageDomainType.USER, user.getUserId());
                        user.updateProfileImageFile(newProfileImageFile);
                    } else {
                        // 기존 프로필 이미지 파일이 있다면 삭제 처리 (선택 사항)
                        if (user.getProfileImageFile() != null) {
                            imageFileService.deleteImage(user.getProfileImageFile().getImageFileId());
                        }
                        user.updateProfileImageFile(null);
                    }
                    log.info("Existing normal user linked with OAuth: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
                } else if (!user.getOauthProvider().equals(oauthProvider)) {
                    log.error("Email conflict during OAuth login: Email {} already exists with other Provider {}", email, user.getOauthProvider());
                    throw new OAuth2AuthenticationException("이미 존재하는 이메일입니다. 다른 방식으로 로그인해주세요.");
                } else {
                    log.error("Unexpected duplicate OAuth user detected: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
                    throw new OAuth2AuthenticationException("알 수 없는 오류로 이미 등록된 계정입니다.");
                }
            } else {
                String generatedPassword = UUID.randomUUID().toString();
                ImageFile profileImageFile = null;

                user = User.builder()
                        .email(email)
                        .password(generatedPassword)
                        .nickname(nickname)
                        .oauthProvider(oauthProvider)
                        .oauthId(oauthId)
                        .role(UserRole.USER)
                        .subscriptionStatus("FREE")
                        .build();
                user = userRepository.save(user); // 먼저 user 저장하여 userId를 얻음

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    // ⭐ 변경된 부분: createExternalImageFile 호출
                    profileImageFile = imageFileService.createExternalImageFile(profileImageUrl, ImageDomainType.USER, user.getUserId());
                    user.updateProfileImageFile(profileImageFile); // User에 ImageFile 연결
                    userRepository.save(user); // User 업데이트
                }
                log.info("New OAuth user created: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
            }
        }
        return userRepository.save(user); // 변경사항이 있으면 저장
    }
}
