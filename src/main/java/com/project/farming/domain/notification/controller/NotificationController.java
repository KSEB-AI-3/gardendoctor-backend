package com.project.farming.domain.notification.controller;

import com.project.farming.domain.notification.dto.NotificationRequestDto;
import com.project.farming.domain.notification.dto.NotificationResponseDto;
import com.project.farming.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "사용자 알림 전체 조회", description = "특정 사용자의 알림을 최신순으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponseDto>> getUserNotifications(
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @Operation(summary = "사용자 알림 생성 및 FCM 발송", description = "특정 사용자에게 알림을 생성하고 FCM 푸시 알림을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 생성 및 발송 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/{userId}")
    public ResponseEntity<Void> createNotification(
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
            @RequestBody NotificationRequestDto requestDto) {
        notificationService.createAndSendNotification(userId, requestDto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "단일 알림 읽음 처리", description = "알림 ID를 기반으로 단일 알림을 읽음 상태로 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "알림 ID", example = "10") @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 알림 전체 삭제", description = "특정 사용자의 모든 알림을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 전체 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAllUserNotifications(
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId) {
        notificationService.deleteAllUserNotifications(userId);
        return ResponseEntity.ok().build();
    }
}
