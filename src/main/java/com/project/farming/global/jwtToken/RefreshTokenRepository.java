package com.project.farming.global.jwtToken;

import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Refresh Token 문자열 값으로 엔티티 조회 메소드 추가
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    void deleteByUser(User user); // 로그아웃, refresh 토큰 갱신

    // upsert를 위한 네이티브 쿼리 메서드 추가
    @Modifying
    @Query(value = "INSERT INTO refresh_token (user_pk, refresh_token, expires_at, created_at) " +
            "VALUES (:user_pk, :refresh_token, :expires_at, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "refresh_token = VALUES(refresh_token), expires_at = VALUES(expires_at)", nativeQuery = true)
    void saveOrUpdate(@Param("user_pk") Long userPk,
                      @Param("refresh_token") String refreshToken,
                      @Param("expires_at") Instant expiresAt);

}