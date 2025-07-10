// src/main/java/com/project/farming/global/jwtToken/JwtTokenProvider.java (예시)
package com.project.farming.global.jwtToken;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration; // 밀리초

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration; // 밀리초

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // ⭐ userId (Long)를 받도록 변경
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // ⭐ Long을 String으로 변환하여 Subject에 설정
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ⭐ userId (Long)를 받도록 변경
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // ⭐ Long을 String으로 변환하여 Subject에 설정
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ⭐ Long 타입의 userId를 반환하도록 변경
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject()); // ⭐ Subject를 Long으로 변환하여 반환
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException ex) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException ex) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException ex) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException ex) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    public long getExpirationRemainingTimeMillis(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expirationDate = claims.getExpiration();
            return expirationDate.getTime() - System.currentTimeMillis();
        } catch (ExpiredJwtException ex) {
            // 토큰이 이미 만료된 경우 남은 시간은 0 또는 음수
            return 0;
        } catch (Exception ex) {
            log.error("JWT 토큰 만료 시간 계산 중 오류 발생: {}", ex.getMessage());
            return -1; // 오류를 나타내는 값
        }
    }
}