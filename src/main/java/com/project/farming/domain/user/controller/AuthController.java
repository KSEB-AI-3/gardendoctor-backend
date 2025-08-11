package com.project.farming.domain.user.controller;

import com.project.farming.domain.user.dto.*;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.exception.UserNotFoundException;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageFile;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

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

    /**
     * ✅ FCM 토큰 갱신 API 추가
     */
    @Operation(summary = "FCM 토큰 갱신", description = "로그인된 사용자의 FCM 기기 토큰을 갱신합니다. 앱 시작 시 또는 토큰 갱신 시 호출해야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "jwtAuth")
    @PatchMapping("/fcm-token")
    public ResponseEntity<AuthResponseDto> updateFcmToken(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody FcmTokenUpdateRequestDto requestDto) {

        Long userId = customUserDetails.getUser().getUserId();
        authService.updateFcmToken(userId, requestDto.getFcmToken());

        return ResponseEntity.ok(AuthResponseDto.builder().message("FCM 토큰이 성공적으로 갱신되었습니다.").build());
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
        Long userId = customUserDetails.getUser().getUserId();

        authService.logout(accessToken, userId);

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
                    content = @Content(schema = @Schema(implementation = UserMyPageResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @GetMapping("/user/me")
    public ResponseEntity<UserMyPageResponseDto> getCurrentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getUserId();
        UserMyPageResponseDto responseDto = authService.getMyPageInfo(userId);

        return ResponseEntity.ok(responseDto);
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


    @Operation(summary = "닉네임 변경", description = "로그인한 사용자의 닉네임을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 변경 성공",
                    content = @Content(schema = @Schema(implementation = UserMyPageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class)))
    })
    @SecurityRequirement(name = "jwtAuth")
    @PatchMapping("/me/nickname")
    public ResponseEntity<UserMyPageResponseDto> updateNickname(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody NicknameUpdateRequestDto requestDto) {
        Long userId = customUserDetails.getUser().getUserId();
        UserMyPageResponseDto updated = authService.updateNickname(userId, requestDto.getNewNickname());
        return ResponseEntity.ok(updated);
    }

    // 이미지 변경
    @Operation(summary = "프로필 이미지 업로드 및 변경", description = "사용자가 업로드한 이미지 파일로 프로필 이미지를 변경합니다.")
    @SecurityRequirement(name = "jwtAuth")
    @PatchMapping(value = "/me/profile-image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserMyPageResponseDto> updateProfileImageByUpload(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart("image") MultipartFile imageFile
    ) {
        Long userId = customUserDetails.getUser().getUserId();
        UserMyPageResponseDto updated = authService.updateProfileImage(userId, imageFile);
        return ResponseEntity.ok(updated);
    }

    // 프로필 이미지 삭제 (기본 이미지로 되돌리기)
    @Operation(summary = "프로필 이미지 삭제", description = "로그인한 사용자의 프로필 이미지를 기본 이미지로 되돌립니다.")
    @SecurityRequirement(name = "jwtAuth")
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<UserMyPageResponseDto> deleteProfileImage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getUserId();
        UserMyPageResponseDto updated = authService.deleteProfileImage(userId);
        return ResponseEntity.ok(updated);
    }

}