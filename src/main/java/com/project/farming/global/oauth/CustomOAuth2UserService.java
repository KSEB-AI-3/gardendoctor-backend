package com.project.farming.global.oauth;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.entity.UserRole; // UserRole 임포트 유지
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.jwtToken.CustomUserDetails; // CustomUserDetails 임포트 유지
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("OAuth2 User Attributes: {}", oAuth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String oauthId; // 각 소셜 서비스에서 제공하는 고유 ID
        String email;
        String nickname = null;
        String profileImage = null;

        if ("google".equals(registrationId)) {
            // Google은 'sub' (String)이 고유 ID로, oAuth2User.getName()이 이를 반환
            oauthId = oAuth2User.getName();
            email = oAuth2User.getAttribute("email");
            nickname = oAuth2User.getAttribute("name");
            profileImage = oAuth2User.getAttribute("picture");
        } else if ("kakao".equals(registrationId)) {
            // ⭐ Kakao는 'id' (Long)가 고유 ID입니다. ClassCastException 방지를 위해 String.valueOf() 사용
            Long kakaoId = oAuth2User.getAttribute("id");
            oauthId = String.valueOf(kakaoId);

            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                // 카카오 이메일은 동의 여부와 함께 제공될 수 있으므로, 해당 필드를 확인하여 가져옵니다.
                Boolean hasEmail = (Boolean) kakaoAccount.get("has_email");
                Boolean emailNeedsAgreement = (Boolean) kakaoAccount.get("email_needs_agreement");

                if (hasEmail != null && hasEmail && (emailNeedsAgreement == null || !emailNeedsAgreement)) {
                    email = (String) kakaoAccount.get("email");
                } else {
                    email = null; // 이메일 동의를 안 했거나 정보가 없는 경우
                }

                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                    profileImage = (String) profile.get("profile_image_url");
                }
            } else {
                email = null; // kakao_account 자체가 없는 경우 (드뭄)
            }
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = oAuth2User.getAttribute("response");
            if (response != null) {
                oauthId = (String) response.get("id"); // Naver는 response 안의 'id' (String)가 고유 ID
                email = (String) response.get("email");
                nickname = (String) response.get("nickname");
                profileImage = (String) response.get("profile_image");
            } else {
                log.warn("Naver OAuth2 response attributes are missing.");
                oauthId = null; // 필수 정보 누락으로 간주
                email = null;
            }
        } else {
            log.warn("Unsupported OAuth2 registrationId: {}", registrationId);
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다.");
        }

        // 이메일이 필수라고 가정했으므로, 이메일이 없으면 예외 발생
        // ⭐ BaseException 대신 OAuth2AuthenticationException 사용
        if (email == null || email.trim().isEmpty()) {
            log.error("OAuth2 Login Failed: Email not provided by {} for oauthId {}", registrationId, oauthId);
            throw new OAuth2AuthenticationException("이메일 정보는 필수입니다. 이메일 제공에 동의해주세요.");
        }

        // 사용자 정보를 DB에 저장하거나 업데이트 (UserRole 로직 포함)
        User user = saveOrUpdateOAuthUser(oauthId, email, nickname, profileImage, registrationId);

        // 핵심: CustomUserDetails를 반환하여 Spring Security에 사용자 정의 User 정보 제공
        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }

    private User saveOrUpdateOAuthUser(String oauthId, String email, String nickname, String profileImage, String oauthProvider) {
        Optional<User> optionalUser;

        // 1. oauthProvider와 oauthId로 기존 소셜 사용자 조회
        optionalUser = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);

        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            // 기존 소셜 사용자는 닉네임과 프로필 이미지만 업데이트 (이메일은 변경 불가능)
            user.updateNickname(nickname);
            user.updateProfileImage(profileImage);
            log.info("Existing OAuth user updated: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
        } else {
            // 2. 이메일로 기존 일반 사용자 또는 다른 소셜 사용자 조회 (이메일 중복 방지)
            optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                // ⭐ 중요: 이미 이메일이 존재하고 다른 방식으로 가입된 경우
                if (user.getOauthProvider() == null || user.getOauthProvider().isEmpty()) {
                    // 기존 일반 회원이라면, 이메일이 일치하므로 해당 계정에 소셜 정보 연동
                    user.setOauthProvider(oauthProvider);
                    user.setOauthId(oauthId);
                    user.updateNickname(nickname); // 닉네임, 프로필 이미지 업데이트
                    user.updateProfileImage(profileImage);
                    log.info("Existing normal user linked with OAuth: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
                } else if (!user.getOauthProvider().equals(oauthProvider)) {
                    // 동일 이메일로 다른 소셜 계정으로 이미 가입된 경우
                    // ⭐ BaseException 대신 OAuth2AuthenticationException 사용
                    log.error("Email conflict during OAuth login: Email {} already exists with other Provider {}", email, user.getOauthProvider());
                    throw new OAuth2AuthenticationException("이미 존재하는 이메일입니다. 다른 방식으로 로그인해주세요.");
                } else {
                    // 이미 같은 소셜 계정으로 가입된 경우 (위의 findByOauthProviderAndOauthId에서 걸러져야 함)
                    log.error("Unexpected duplicate OAuth user detected: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
                    throw new OAuth2AuthenticationException("알 수 없는 오류로 이미 등록된 계정입니다.");
                }
            } else {
                // 3. 완전히 새로운 사용자 생성
                String generatedPassword = UUID.randomUUID().toString();

                user = User.builder()
                        .email(email)
                        .password(generatedPassword)
                        .nickname(nickname)
                        .profileImage(profileImage)
                        .oauthProvider(oauthProvider)
                        .oauthId(oauthId)
                        .role(UserRole.USER) // 새로운 사용자에게 UserRole.USER 역할 부여
                        .subscriptionStatus("FREE")
                        .build();
                log.info("New OAuth user created: Email={}, Provider={}, OAuthId={}", email, oauthProvider, oauthId);
            }
        }
        return userRepository.save(user);
    }
}