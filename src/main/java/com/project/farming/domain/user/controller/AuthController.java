package com.project.farming.domain.user.controller;

import com.project.farming.domain.user.dto.AuthResponseDto;
import com.project.farming.domain.user.dto.LoginRequestDto;
import com.project.farming.domain.user.dto.RegisterRequestDto;
import com.project.farming.domain.user.dto.TokenRefreshRequestDto;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.jwtToken.JwtToken;
import com.project.farming.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation; // ⭐ Swagger Operation 임포트
import io.swagger.v3.oas.annotations.Parameter; // ⭐ Swagger Parameter 임포트
import io.swagger.v3.oas.annotations.media.Content; // ⭐ Swagger Content 임포트
import io.swagger.v3.oas.annotations.media.Schema; // ⭐ Swagger Schema 임포트
import io.swagger.v3.oas.annotations.parameters.RequestBody; // ⭐ Swagger RequestBody 임포트
import io.swagger.v3.oas.annotations.responses.ApiResponse; // ⭐ Swagger ApiResponse 임포트
import io.swagger.v3.oas.annotations.responses.ApiResponses; // ⭐ Swagger ApiResponses 임포트
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // ⭐ Swagger SecurityRequirement 임포트
import io.swagger.v3.oas.annotations.tags.Tag; // ⭐ Swagger Tag 임포트
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증 (Auth)", description = "회원가입, 로그인, 토큰 관리 등 사용자 인증 관련 API") // ⭐ 컨트롤러에 Tag 추가
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. 이메일은 고유해야 합니다.") // ⭐ API 설명
    @ApiResponses(value = { // ⭐ 응답 정의
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패 또는 이메일 중복)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @RequestBody(description = "회원가입 요청 정보", required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequestDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequestDto request) {
        User registeredUser = authService.registerUser(request.getEmail(), request.getPassword(), request.getNickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponseDto.builder()
                        .message("회원가입이 성공적으로 완료되었습니다.")
                        .data(registeredUser.getEmail())
                        .build()
        );
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = JwtToken.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<JwtToken> login(
            @RequestBody(description = "로그인 요청 정보", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequestDto request) {
        JwtToken tokens = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(tokens);
    }

    @Operation(summary = "로그아웃", description = "현재 사용자의 Access Token을 블랙리스트에 추가하고 Refresh Token을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효한 토큰 없음)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth") // ⭐ 이 API는 JWT 인증 필요
    @PostMapping("/logout")
    public ResponseEntity<AuthResponseDto> logout(
            HttpServletRequest request,
            @Parameter(description = "인증된 사용자 ID", example = "1") // ⭐ 파라미터 설명
            @AuthenticationPrincipal Long userId) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String accessToken = header.substring(7);
            authService.logout(accessToken, userId);
            return ResponseEntity.ok(AuthResponseDto.builder().message("로그아웃 성공").build());
        }
        return ResponseEntity.badRequest().body(AuthResponseDto.builder().message("로그아웃 실패: 유효한 토큰이 필요합니다.").build());
    }

    @Operation(summary = "토큰 재발급", description = "만료된 Access Token 대신 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = JwtToken.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않거나 블랙리스트에 있는 리프레시 토큰)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<JwtToken> refreshToken(
            @RequestBody(description = "리프레시 토큰", required = true,
                    content = @Content(schema = @Schema(implementation = TokenRefreshRequestDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody TokenRefreshRequestDto request) {
        JwtToken newTokens = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(newTokens);
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 사용자의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth") // ⭐ 이 API는 JWT 인증 필요
    @GetMapping("/user/me")
    public ResponseEntity<User> getCurrentUser(
            @Parameter(description = "인증된 사용자 ID", example = "1") // ⭐ 파라미터 설명
            @AuthenticationPrincipal Long userId) {
        return authService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
