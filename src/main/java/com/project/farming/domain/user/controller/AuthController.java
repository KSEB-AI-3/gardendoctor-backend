// src/main/java/com/project/urbanfarm/controller/AuthController.java
package com.project.farming.domain.user.controller;


import com.project.farming.domain.user.dto.AuthResponseDto;
import com.project.farming.domain.user.dto.LoginRequestDto;
import com.project.farming.domain.user.dto.RegisterRequestDto;
import com.project.farming.domain.user.dto.TokenRefreshRequestDto;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.jwtToken.JwtToken; // ⭐ JwtToken DTO 임포트
import jakarta.servlet.http.HttpServletRequest;
import com.project.farming.domain.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ⭐ @AuthenticationPrincipal 임포트
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        User registeredUser = authService.registerUser(request.getEmail(), request.getPassword(), request.getNickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponseDto.builder()
                        .message("회원가입이 성공적으로 완료되었습니다.")
                        .data(registeredUser.getEmail())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<JwtToken> login(@Valid @RequestBody LoginRequestDto request) { // ⭐ 응답 타입을 JwtToken으로 변경
        JwtToken tokens = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(tokens); // ⭐ 직접 JwtToken 객체 반환
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponseDto> logout(HttpServletRequest request,
                                                  @AuthenticationPrincipal String userEmail) { // ⭐ @AuthenticationPrincipal로 userEmail 받기
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);
            authService.logout(accessToken, userEmail); // ⭐ userEmail 전달
            return ResponseEntity.ok(AuthResponseDto.builder().message("로그아웃 성공").build());
        }
        return ResponseEntity.badRequest().body(AuthResponseDto.builder().message("로그아웃 실패: 유효한 토큰이 필요합니다.").build());
    }

    @PostMapping("/token/refresh") // ⭐ 경로 변경 (일관성을 위해)
    public ResponseEntity<JwtToken> refreshToken(@Valid @RequestBody TokenRefreshRequestDto request) { // ⭐ 응답 타입을 JwtToken으로 변경
        JwtToken newTokens = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(newTokens); // ⭐ 직접 JwtToken 객체 반환
    }

    @GetMapping("/user/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal String userEmail) {
        return authService.getUserByEmail(userEmail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}