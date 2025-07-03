package com.project.farming.global.oauth;

import com.project.farming.global.jwtToken.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    private String redirectUrlAfterLogin = "http://localhost:3000/oauth2/redirect"; // ⭐ 실제 앱의 콜백 URL로 변경하세요.

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String userId = getUserIdentifierFromOAuth2User(oAuth2User);

        String accessToken = jwtTokenProvider.generateToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        log.info("OAuth2 Login Success: User Identifier = {}", userId);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrlAfterLogin)
                .queryParam("accessToken", URLEncoder.encode(accessToken, StandardCharsets.UTF_8.toString()))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8.toString()))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getUserIdentifierFromOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email != null) {
            return email;
        }

        Object kakaoIdObj = oAuth2User.getAttribute("id");
        if (kakaoIdObj != null) {
            return String.valueOf(kakaoIdObj);
        }

        return oAuth2User.getName();
    }
}