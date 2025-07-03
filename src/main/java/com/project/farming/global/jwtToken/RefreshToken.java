package com.project.farming.global.jwtToken;

import com.project.farming.domain.user.entity.User; // User 엔티티의 정확한 경로
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.Instant; // ⭐ Instant 사용을 위해 추가

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token", columnList = "refreshToken") // refreshToken 필드에 인덱스 추가
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 엔티티를 위한 protected 기본 생성자
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512) // 리프레시 토큰 문자열, null 불가능, 길이 넉넉하게 설정
    private String refreshToken;

    @ManyToOne(fetch = FetchType.LAZY) // User와 N:1 관계, 지연 로딩
    @JoinColumn(name = "user_pk", nullable = false) // user_pk 컬럼으로 조인, null 불가능
    private User user;

    @Column(nullable = false, updatable = false) // 생성 시에만 설정되도록 updatable=false 추가
    private Timestamp createdAt; // 토큰 생성 시간 (java.sql.Timestamp)

    // ⭐ 추가 제안: Refresh Token의 실제 만료 시간을 DB에 저장
    @Column(nullable = false)
    private Instant expiresAt; // 토큰 만료 시간 (Java 8의 Instant 사용 권장)

    @PrePersist // 엔티티가 영속화되기 전에 실행
    protected void onCreate() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        // ⭐ expiresAt은 JwtTokenProvider에서 토큰 생성 시 계산하여 설정해야 합니다.
        // 이 메서드에서는 일반적으로 설정하지 않습니다.
    }

    // ⭐ 리프레시 토큰 문자열 업데이트 메서드 (Refresh Token Rotation 시 사용)
    public void updateRefreshToken(String newRefreshToken, Instant newExpiresAt) {
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
        // createdAt은 변경하지 않습니다.
    }
}