package com.project.farming.global.oauth;

import com.project.farming.domain.user.entity.User; // User 임포트 추가
import com.project.farming.domain.user.repository.UserRepository; // UserRepository 임포트 추가
import com.project.farming.global.jwtToken.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository; // ⭐ UserRepository 주입

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = null;
        if (authentication instanceof OAuth2AuthenticationToken) {
            registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        }

        // ⭐ CustomOAuth2UserService에서 DB에 저장된 User 객체를 가져옵니다.
        // CustomOAuth2UserService가 CustomUserDetails를 반환하고,
        // CustomUserDetails가 User 객체를 가지고 있다면 다음과 같이 접근할 수 있습니다.
        User user = null;
        if (authentication.getPrincipal() instanceof com.project.farming.global.jwtToken.CustomUserDetails) {
            user = ((com.project.farming.global.jwtToken.CustomUserDetails) authentication.getPrincipal()).getUser();
        } else {
            // 만약 CustomUserDetails가 아닌 기본 OAuth2User를 반환한다면,
            // 여기서 다시 DB에서 사용자를 찾아야 합니다.
            // 이메일 또는 OAuth ID를 사용하여 사용자를 찾습니다.
            String oauthId = getOAuthIdFromOAuth2User(oAuth2User, registrationId);
            String email = getEmailFromOAuth2User(oAuth2User, registrationId); // 이메일도 가져오는 헬퍼 메서드 추가 필요

            if (oauthId != null && registrationId != null) {
                user = userRepository.findByOauthProviderAndOauthId(registrationId, oauthId)
                        .orElse(null); // 또는 예외 처리
            }
            // 이메일로도 찾아볼 수 있습니다.
            if (user == null && email != null) {
                user = userRepository.findByEmail(email).orElse(null);
            }
        }

        if (user == null) {
            log.error("OAuth2 Login Success Handler: User not found in DB after successful authentication.");
            // 사용자 정보를 찾지 못했으므로, 에러 페이지로 리다이렉트하거나 적절히 처리
            getRedirectStrategy().sendRedirect(request, response, "/login-error");
            return;
        }

        // ⭐ JWT 토큰에 DB의 userId(PK)를 사용
        String accessToken = jwtTokenProvider.generateToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        log.info("OAuth2 Login Success: User ID (PK) = {}", user.getUserId());
        log.info("Generated Access Token: {}", accessToken);
        log.info("Generated Refresh Token: {}", refreshToken);

        String redirectUrlAfterLogin = "http://localhost:8080/login-success";

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrlAfterLogin)
                .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // ⭐ OAuth2User 객체에서 각 서비스별 고유 ID (oauthId) 추출 (기존과 동일)
    private String getOAuthIdFromOAuth2User(OAuth2User oAuth2User, String registrationId) {
        if ("google".equals(registrationId)) {
            return oAuth2User.getName();
        } else if ("kakao".equals(registrationId)) {
            Object id = oAuth2User.getAttribute("id");
            return id != null ? String.valueOf(id) : null;
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = oAuth2User.getAttribute("response");
            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        }
        log.warn("Could not find a unique OAuth ID for provider {} with attributes: {}", registrationId, oAuth2User.getAttributes());
        return null;
    }

    // ⭐ OAuth2User 객체에서 이메일 추출 헬퍼 메서드 추가 (필요시)
    private String getEmailFromOAuth2User(OAuth2User oAuth2User, String registrationId) {
        if ("google".equals(registrationId)) {
            return oAuth2User.getAttribute("email");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
                return (String) kakaoAccount.get("email");
            }
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = oAuth2User.getAttribute("response");
            if (response != null && response.containsKey("email")) {
                return (String) response.get("email");
            }
        }
        return null;
    }
}