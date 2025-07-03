package com.project.farming.global.oauth;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

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
        String email = null;
        String nickname = null;
        String profileImage = null;

        if ("google".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            nickname = oAuth2User.getAttribute("name");
            profileImage = oAuth2User.getAttribute("picture");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                    profileImage = (String) profile.get("profile_image_url");
                }
            }
        }

        User user = saveOrUpdateOAuthUser(email, nickname, profileImage, registrationId);

        return oAuth2User;
    }

    private User saveOrUpdateOAuthUser(String email, String nickname, String profileImage, String oauthProvider) {
        Optional<User> optionalUser = userRepository.findByEmailAndOauthProvider(email, oauthProvider);
        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user.setNickname(nickname);
            user.setProfileImage(profileImage);
            user.setOauthProvider(oauthProvider);
        } else {
            user = User.builder()
                    .email(email)
                    .nickname(nickname)
                    .profileImage(profileImage)
                    .oauthProvider(oauthProvider)
                    .password("") // 소셜 로그인 사용자는 비밀번호 없음
                    .subscriptionStatus("FREE")
                    .build();
        }
        return userRepository.save(user);
    }
}