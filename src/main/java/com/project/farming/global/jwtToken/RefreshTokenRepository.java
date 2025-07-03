package com.project.farming.global.jwtToken;

import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user); // 기존 메소드 (필요시 사용)

    // Refresh Token 문자열 값으로 엔티티 조회 메소드 추가
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    void deleteByUser(User user); // 로그아웃 등에서 사용

    boolean existsByRefreshToken(String refreshToken); // 기존 메소드

    void deleteByRefreshToken(String refreshToken); // Refresh Token Rotation 등에서 사용 가능
}