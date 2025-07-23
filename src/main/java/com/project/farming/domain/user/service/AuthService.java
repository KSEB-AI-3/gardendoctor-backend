package com.project.farming.domain.user.service;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.entity.UserRole;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.UserNotFoundException;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
                .role(UserRole.USER)
                .subscriptionStatus("FREE")
                .build();
        return userRepository.save(newUser);
    }

    @Transactional
    public JwtToken login(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // 인증된 사용자의 이메일로 User 객체를 찾고, User의 PK(userId)를 토큰에 사용
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("인증된 사용자를 찾을 수 없습니다."));

            String accessToken = jwtTokenProvider.generateToken(user.getUserId());
            String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUserId());
            long refreshTokenExpirationMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(refreshTokenString);

            refreshTokenRepository.deleteByUser(user);

            RefreshToken refreshToken = RefreshToken.builder()
                    .refreshToken(refreshTokenString)
                    .user(user)
                    .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMillis))
                    .build();
            refreshTokenRepository.save(refreshToken);

            return JwtToken.builder()
                    .grantType("Bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenString)
                    .build();
        } catch (AuthenticationException e) {
            log.warn("로그인 실패: 이메일 또는 비밀번호가 잘못되었습니다. 이메일: {}", email, e.getMessage());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
    }

    @Transactional
    public void logout(String accessToken, Long userId) {
        long remainingTimeMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(accessToken);
        if (remainingTimeMillis > 0) {
            jwtBlacklistService.blacklistToken(accessToken, remainingTimeMillis);
            log.info("Access Token 블랙리스트 등록 완료. 토큰: {}", accessToken);
        } else {
            log.warn("만료되었거나 유효하지 않은 Access Token이 로그아웃 요청에 사용되었습니다: {}", accessToken);
        }

        // Refresh Token 삭제
        userRepository.findById(userId).ifPresentOrElse(user -> { // <-- 여기서부터 차이 발생
            refreshTokenRepository.deleteByUser(user);
            log.info("사용자 {} (ID: {})의 Refresh Token 삭제를 시도했습니다.", user.getEmail(), userId);
        }, () -> {
            log.warn("로그아웃 요청: 사용자 ID {}를 찾을 수 없습니다. 해당 사용자는 존재하지 않거나 이미 삭제되었습니다.", userId);
            throw new UserNotFoundException("로그아웃하려는 사용자를 찾을 수 없습니다.");
        });
    }

    @Transactional
    public JwtToken refreshTokens(String refreshTokenString) {
        if (!jwtTokenProvider.validateToken(refreshTokenString)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // JWT에서 직접 userId (Long)를 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshTokenString);

        if (jwtBlacklistService.isBlacklisted(refreshTokenString)) {
            log.warn("블랙리스트에 등록된 리프레시 토큰이 재사용되었습니다. 사용자 ID {}의 모든 리프레시 토큰을 삭제합니다.", userId);
            userRepository.findById(userId)
                    .ifPresent(user -> refreshTokenRepository.deleteByUser(user));
            throw new IllegalArgumentException("비정상적인 접근입니다. 재로그인이 필요합니다.");
        }

        // userId (Long)로 User 객체 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        RefreshToken storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshTokenString)
                .orElseThrow(() -> new IllegalArgumentException("DB에 존재하지 않는 리프레시 토큰입니다. 재로그인이 필요합니다."));

        // DB에 저장된 토큰이 현재 사용자에게 할당된 것인지 추가 확인 (userId로 비교)
        if (!storedRefreshToken.getUser().getUserId().equals(userId)) {
            log.warn("리프레시 토큰 소유자 불일치: 요청 토큰의 사용자 ID({})와 DB 토큰의 사용자 ID({})", userId, storedRefreshToken.getUser().getUserId());
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다. 재로그인이 필요합니다.");
        }

        String newAccessToken = jwtTokenProvider.generateToken(userId);
        String newRefreshTokenString = jwtTokenProvider.generateRefreshToken(userId);
        long newRefreshTokenExpirationMillis = jwtTokenProvider.getExpirationRemainingTimeMillis(newRefreshTokenString);

        long oldRefreshTokenRemainingTime = jwtTokenProvider.getExpirationRemainingTimeMillis(refreshTokenString);
        if (oldRefreshTokenRemainingTime > 0) {
            jwtBlacklistService.blacklistToken(refreshTokenString, oldRefreshTokenRemainingTime);
            log.info("이전 Refresh Token 블랙리스트 등록 완료: {}", refreshTokenString);
        } else {
            log.warn("이미 만료된 Refresh Token이 재발급 요청에 사용되었습니다. 재로그인 필요: {}", refreshTokenString);
            throw new IllegalArgumentException("만료된 Refresh Token입니다. 재로그인이 필요합니다.");
        }

        storedRefreshToken.updateRefreshToken(newRefreshTokenString, Instant.now().plusMillis(newRefreshTokenExpirationMillis));
        refreshTokenRepository.save(storedRefreshToken);

        log.info("토큰 재발급 성공. 새로운 Access Token 및 Refresh Token 발급.");

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString)
                .build();
    }

    // ⭐ userId (Long)로 사용자를 조회하는 메서드 추가
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
}
