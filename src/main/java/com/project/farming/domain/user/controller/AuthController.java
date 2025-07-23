package com.project.farming.domain.user.controller;

import com.project.farming.domain.user.dto.*;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.jwtToken.CustomUserDetails;
import com.project.farming.global.jwtToken.JwtToken;
import com.project.farming.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증 (Auth)", description = "회원가입, 로그인, 토큰 관리 등 사용자 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. 이메일은 고유해야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패 또는 이메일 중복)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("RegisterRequestDto: {}", request);
        try {
            authService.registerUser(request.getEmail(), request.getPassword(), request.getNickname());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    AuthResponseDto.builder()
                            .message("회원가입이 성공적으로 완료되었습니다.")
                            .data(request.getEmail())
                            .build()
            );
        } catch (IllegalArgumentException ex) {
            log.warn("회원가입 실패: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                    AuthResponseDto.builder()
                            .message(ex.getMessage())
                            .errorCode("BAD_REQUEST")
                            .build()
            );
        }
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
    public ResponseEntity<JwtToken> login(@Valid @RequestBody LoginRequestDto request) {
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
    @SecurityRequirement(name = "jwtAuth")
    @PostMapping("/logout")
    public ResponseEntity<AuthResponseDto> logout(
            HttpServletRequest request,
            @Parameter(description = "인증된 사용자 ID", example = "1")
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        String header = request.getHeader("Authorization");

        // 1. Authorization 헤더 및 Bearer 토큰 형식 검증
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("로그아웃 실패: Authorization 헤더가 없거나 'Bearer ' 형식으로 시작하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthResponseDto.builder()
                            .message("로그아웃 실패: 유효한 Access Token이 필요합니다.")
                            .errorCode("UNAUTHORIZED")
                            .build()
            );
        }

        String accessToken = header.substring(7);
        Long userId = customUserDetails.getUser().getUserId(); // @AuthenticationPrincipal이 이미 인증된 사용자 정보를 제공

        // 2. AuthService 호출
        authService.logout(accessToken, userId);

        // 3. 성공 응답 (이미 토큰이 없었어도 사용자에게는 성공으로 간주)
        return ResponseEntity.ok(AuthResponseDto.builder().message("로그아웃 성공").build());
    }

    @Operation(summary = "토큰 재발급", description = "만료된 Access Token 대신 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = JwtToken.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않거나 블랙리스트에 있는 리프레시 토큰)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<JwtToken> refreshToken(@Valid @RequestBody TokenRefreshRequestDto request) {
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
    @SecurityRequirement(name = "jwtAuth")
    @GetMapping("/user/me")
    public ResponseEntity<User> getCurrentUser(
            @Parameter(hidden = true) // Swagger 문서에서 이 파라미터를 숨깁니다.
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getUserId();
        return authService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "내 정보 페이지 조회", description = "로그인한 사용자의 마이페이지 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "마이페이지 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserMyPageResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @GetMapping("/me")
    public ResponseEntity<UserMyPageResponseDto> getMyPage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getUserId();
        UserMyPageResponseDto response = authService.getMyPageInfo(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 정보 수정", description = "로그인한 사용자의 닉네임, 프로필 이미지, FCM 토큰, 구독 상태를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 정보 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserMyPageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패 또는 존재하지 않는 이미지 파일)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @PatchMapping("/me")
    public ResponseEntity<UserMyPageResponseDto> updateMyPage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UserMyPageUpdateRequestDto requestDto) {
        Long userId = customUserDetails.getUser().getUserId();
        UserMyPageResponseDto updated = authService.updateMyPageInfo(userId, requestDto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자의 계정을 삭제합니다. (하드 삭제)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공 (No Content)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyPage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getUserId();
        authService.deleteMyPageInfo(userId);
        return ResponseEntity.noContent().build();
    }
}
