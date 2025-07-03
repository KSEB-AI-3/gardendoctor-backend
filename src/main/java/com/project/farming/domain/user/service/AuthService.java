// src/main/java/com/project/farming/domain/user/service/AuthService.java
package com.project.farming.domain.user.service;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.jwtToken.JwtBlacklistService;
import com.project.farming.global.jwtToken.JwtToken;
import com.project.farming.global.jwtToken.JwtTokenProvider;
import com.project.farming.global.jwtToken.RefreshToken;
import com.project.farming.global.jwtToken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // UsernameNotFoundException 사용
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public User registerUser(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        User newUser = User.builder()
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode(password)) // 비밀번호 암호화
                .oauthProvider("LOCAL") // 자체 회원가입은 "LOCAL"로 구분
                .subscriptionStatus("FREE")
                .build();
        return userRepository.save(newUser);
    }

    @Transactional
    public JwtToken login(String email, String password) {
        try {
            // Spring Security를 통한 사용자 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            String authenticatedEmail = authentication.getName(); // 인증된 사용자의 이메일 (UserDetails의 getUsername())
            User user = userRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("인증된 사용자를 찾을 수 없습니다.")); // UsernameNotFoundException 사용

            // Access Token 및 Refresh Token 생성
            String accessToken = jwtTokenProvider.generateToken(authenticatedEmail);
            String refreshTokenString = jwtTokenProvider.generateRefreshToken(authenticatedEmail);
            long refreshTokenExpirationMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(refreshTokenString);

            // 기존 리프레시 토큰이 있다면 삭제 (다중 로그인 기기 대응: 최신 토큰만 유효하도록)
            refreshTokenRepository.deleteByUser(user);

            // Refresh Token을 DB에 저장
            RefreshToken refreshToken = RefreshToken.builder()
                    .refreshToken(refreshTokenString)
                    .user(user)
                    .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMillis)) // 만료 시간 설정
                    .build();
            refreshTokenRepository.save(refreshToken);

            return JwtToken.builder()
                    .grantType("Bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenString)
                    .build();
        } catch (AuthenticationException e) {
            // Spring Security의 AuthenticationException을 잡고, 더 구체적인 메시지를 반환
            log.warn("로그인 실패: 이메일 또는 비밀번호가 잘못되었습니다. 이메일: {}", email, e.getMessage());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다."); // GlobalExceptionHandler에서 처리될 예외
        }
    }

    @Transactional
    public void logout(String accessToken, String userEmail) {
        // Access Token 블랙리스트 처리
        long remainingTimeMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(accessToken);
        if (remainingTimeMillis > 0) {
            jwtBlacklistService.blacklistToken(accessToken, remainingTimeMillis);
            log.info("Access Token 블랙리스트 등록 완료. 토큰: {}", accessToken);
        } else {
            log.warn("만료되었거나 유효하지 않은 Access Token입니다. 블랙리스트 처리하지 않습니다: {}", accessToken);
        }

        // DB에서 해당 사용자의 Refresh Token 삭제
        userRepository.findByEmail(userEmail).ifPresent(user -> {
            refreshTokenRepository.deleteByUser(user);
            log.info("사용자 {}의 Refresh Token이 DB에서 삭제되었습니다.", userEmail);
        });
    }

    @Transactional
    public JwtToken refreshTokens(String refreshTokenString) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshTokenString)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        // 2. Refresh Token이 블랙리스트에 있는지 확인 (이미 사용된 토큰인지)
        if (jwtBlacklistService.isBlacklisted(refreshTokenString)) {
            throw new IllegalArgumentException("블랙리스트에 등록된 리프레시 토큰입니다. 재로그인이 필요합니다.");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshTokenString);
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId)); // UsernameNotFoundException 사용

        // 3. DB에 저장된 Refresh Token과 일치하는지 확인
        RefreshToken storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshTokenString)
                .orElseThrow(() -> new IllegalArgumentException("DB에 존재하지 않는 리프레시 토큰입니다. 재로그인이 필요합니다."));

        // 4. 새로운 Access Token 및 Refresh Token 생성
        String newAccessToken = jwtTokenProvider.generateToken(userId);
        String newRefreshTokenString = jwtTokenProvider.generateRefreshToken(userId);
        long newRefreshTokenExpirationMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(newRefreshTokenString);

        // 5. 기존 리프레시 토큰을 블랙리스트에 추가 (한 번만 사용 가능하도록)
        long oldRefreshTokenRemainingTime = jwtTokenProvider.getExpirationRemainingTimeMillis(refreshTokenString);
        if (oldRefreshTokenRemainingTime > 0) {
            jwtBlacklistService.blacklistToken(refreshTokenString, oldRefreshTokenRemainingTime);
            log.info("이전 Refresh Token 블랙리스트 등록 완료: {}", refreshTokenString);
        } else {
            log.warn("이미 만료된 Refresh Token이 재발급 요청에 사용되었습니다. 재로그인 필요: {}", refreshTokenString);
            throw new IllegalArgumentException("만료된 Refresh Token입니다. 재로그인이 필요합니다.");
        }

        // 6. DB에 새 Refresh Token으로 업데이트 (기존 엔티티 삭제 후 새 엔티티 저장)
        refreshTokenRepository.deleteByRefreshToken(refreshTokenString); // 이전 리프레시 토큰 삭제

        RefreshToken newRefreshToken = RefreshToken.builder()
                .refreshToken(newRefreshTokenString)
                .user(user)
                .expiresAt(Instant.now().plusMillis(newRefreshTokenExpirationMillis))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("토큰 재발급 성공. 새로운 Access Token 및 Refresh Token 발급.");

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString)
                .build();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
