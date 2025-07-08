package com.project.farming.global.oauth;

import com.project.farming.global.jwtToken.JwtTokenProvider;
import jakarta.servlet.ServletException;
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

    // ⭐ 현재는 로컬 테스트용 임시 URL. 나중에 Flutter 앱의 딥링크 스킴 (예: "myfarmingapp://oauth-callback")으로 변경합니다.
    private String redirectUrlAfterLogin = "http://localhost:8080/login-success";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // OAuth 인증 제공자 ID (google, kakao, naver) 가져오기
        String registrationId = null;
        if (authentication instanceof OAuth2AuthenticationToken) {
            registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        }

        // ⭐ JWT 토큰에 포함될 사용자 식별자 (oauthId) 추출
        // 이 식별자는 CustomOAuth2UserService에서 User 엔티티의 oauthId 필드에 저장된 값과 동일합니다.
        String userIdForToken = getOAuthIdFromOAuth2User(oAuth2User, registrationId);

        // 여기서는 JWT 토큰의 subject로 OAuth ID를 사용하지만,
        // 실제 운영에서는 DB에 저장된 User 엔티티의 PK (userId)를 사용하는 것이 더 일반적입니다.
        // User user = userRepository.findByOauthProviderAndOauthId(registrationId, userIdForToken).orElseThrow(...);
        // String userIdToUse = String.valueOf(user.getUserId()); // 실제 DB의 PK 사용

        String accessToken = jwtTokenProvider.generateToken(userIdForToken);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userIdForToken);

        log.info("OAuth2 Login Success: User Identifier (OAuth ID) = {}", userIdForToken);
        log.info("Generated Access Token: {}", accessToken);
        log.info("Generated Refresh Token: {}", refreshToken);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrlAfterLogin)
                .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8.toString()))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8.toString()))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // ⭐ OAuth2User 객체에서 각 서비스별 고유 ID (oauthId) 추출
    private String getOAuthIdFromOAuth2User(OAuth2User oAuth2User, String registrationId) {
        if ("google".equals(registrationId)) {
            // Google의 경우 'sub' 필드가 고유 ID로 사용됩니다. getName()이 이 값을 반환합니다.
            return oAuth2User.getName();
        } else if ("kakao".equals(registrationId)) {
            // Kakao는 'id' 필드가 고유 ID입니다. long 타입이므로 String으로 변환.
            Object id = oAuth2User.getAttribute("id");
            return id != null ? String.valueOf(id) : null;
        } else if ("naver".equals(registrationId)) {
            // Naver는 'response' 객체 내의 'id' 필드가 고유 ID입니다.
            Map<String, Object> response = oAuth2User.getAttribute("response");
            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        }
        log.warn("Could not find a unique OAuth ID for provider {} with attributes: {}", registrationId, oAuth2User.getAttributes());
        // 고유 ID를 찾지 못했다면 null 또는 예외 처리
        return null;
    }
}