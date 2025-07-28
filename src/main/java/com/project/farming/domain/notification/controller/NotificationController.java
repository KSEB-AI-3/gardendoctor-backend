// src/main/java/com/project/farming/domain/notification/controller/NotificationController.java
package com.project.farming.domain.notification.controller;

import com.project.farming.domain.notification.dto.NotificationRequestDto;
import com.project.farming.domain.notification.dto.NotificationResponseDto;
import com.project.farming.domain.notification.service.NotificationService;
import com.project.farming.global.jwtToken.CustomUserDetails;
import com.project.farming.domain.user.entity.UserRole; // UserRole 임포트
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // Spring Security의 AccessDeniedException 임포트 (권장)
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 현재 로그인한 사용자의 알림 목록 조회
     * GET /api/notifications
     * @param customUserDetails 현재 로그인한 사용자 (인증 정보에서 추출)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 알림 목록 응답 DTO (페이징 처리됨)
     */
    @Operation(summary = "내 알림 목록 조회", description = "현재 로그인된 사용자의 모든 알림 목록을 최신순으로 조회합니다. 페이징을 지원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @ParameterObject
            @Parameter(description = "페이징 정보 (기본: page=0, size=10, sort=createdAt,desc)")
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<NotificationResponseDto> notifications = notificationService.getNotificationsForUser(customUserDetails.getUser(), pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 사용자 알림 생성 및 FCM 발송 (관리자 전용)
     * POST /api/notifications/{userId}
     * @param customUserDetails 알림을 받을 사용자 ID
     * @param requestDto 알림 제목과 내용을 담은 DTO
     * @return 성공 시 200 OK
     */
    @Operation(summary = "사용자 알림 생성 및 FCM 발송 (관리자 전용)", description = "특정 사용자에게 알림을 생성하고 FCM 푸시 알림을 발송합니다. **관리자만 접근 가능합니다.**")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 생성 및 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: userId가 비어있거나 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "요청된 사용자 ID 중 해당하는 유저가 없는 경우")
    })
    @PostMapping("/admin/send")
    public ResponseEntity<Void> createNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails, // 관리자 확인용 추가
            @RequestBody @Valid NotificationRequestDto requestDto) {

        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 현재 로그인한 사용자가 ADMIN 역할인지 확인
        if (customUserDetails.getUser().getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("관리자만 사용자에게 알림을 보낼 수 있습니다.");
        }

        notificationService.createAndSendNotificationFromDto(requestDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 단일 알림 읽음 처리 (현재 로그인한 사용자 본인의 알림만 가능)
     * PATCH /api/notifications/{notificationId}/read
     * @param customUserDetails 현재 로그인한 사용자
     * @param notificationId 읽음 처리할 알림 ID
     * @return 읽음 처리된 알림 응답 DTO
     */
    @Operation(summary = "단일 알림 읽음 처리", description = "알림 ID를 기반으로 현재 로그인된 사용자의 알림을 읽음 상태로 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "해당 알림에 대한 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "읽음 처리할 알림 ID", example = "10") @PathVariable Long notificationId) {

        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        NotificationResponseDto response = notificationService.markNotificationAsRead(notificationId, customUserDetails.getUser());
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자의 읽지 않은 알림 개수 조회
     * GET /api/notifications/unread/count
     * @param customUserDetails 현재 로그인한 사용자
     * @return 읽지 않은 알림 개수
     */
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 로그인된 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽지 않은 알림 개수 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long count = notificationService.countUnreadNotifications(customUserDetails.getUser());
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 알림 삭제 (현재 로그인한 사용자 본인의 알림만 가능)
     * DELETE /api/notifications/{notificationId}
     * @param customUserDetails 현재 로그인한 사용자
     * @param notificationId 삭제할 알림 ID
     * @return 응답 없음 (No Content)
     */
    @Operation(summary = "단일 알림 삭제", description = "특정 ID에 해당하는 알림을 삭제합니다. 현재 로그인된 사용자의 알림만 삭제 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "알림 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "해당 알림에 대한 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "삭제할 알림 ID", example = "10") @PathVariable Long notificationId) {

        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.deleteNotification(notificationId, customUserDetails.getUser());
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 로그인한 사용자의 모든 알림 삭제
     * DELETE /api/notifications/all
     * @param customUserDetails 현재 로그인한 사용자
     * @return 응답 없음 (No Content)
     */
    @Operation(summary = "내 모든 알림 삭제", description = "현재 로그인된 사용자의 모든 알림을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "모든 알림 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.deleteAllUserNotifications(customUserDetails.getUser().getUserId(), customUserDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}