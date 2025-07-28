package com.project.farming.global.oauth;

import com.project.farming.domain.user.entity.User;
import com.project.farming.global.image.repository.ImageFileRepository;
import com.project.farming.global.jwtToken.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // loadUser에 트랜잭션이 필요하면 유지

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuthUserService oauthUserService; // ⭐ 새로 만든 OAuthUserService 주입

    @Override
    @Transactional // loadUser 메서드에 트랜잭션이 필요하다면 유지합니다. (예: CustomUserDetails 생성 전 사용자 정보를 안전하게 가져오는 용도)
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

        // ⭐ 분리된 OAuthUserService의 public 메서드 호출
        User user = oauthUserService.saveOrUpdateUserFromOAuth(oauthId, email, nickname, profileImageUrl, registrationId);

        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }

}