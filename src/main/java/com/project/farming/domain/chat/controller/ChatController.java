package com.project.farming.domain.chat.controller;

import com.project.farming.domain.chat.dto.ChatRequestDto;
import com.project.farming.domain.chat.entity.ChatHistory;
import com.project.farming.domain.chat.service.ChatService;
import com.project.farming.domain.user.dto.UserMyPageResponseDto;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Tag(name = "Chat API", description = "작물 챗봇 질문 및 답변 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "챗봇 질문 전송", description = "사용자의 질문을 Python 챗봇 서버로 보내고 응답을 반환합니다.")
    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> req,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String question = req.getOrDefault("query", "");
        // 인증 정보가 없으면 user = null
        User user = (userDetails != null ? userDetails.getUser() : null);

        String answer = chatService.askPythonAgent(user, question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @Operation(summary = "챗봇 세션 메시지 조회", description = "FastAPI에서 특정 세션의 모든 메시지를 조회합니다.")
    @GetMapping("/history/messages")
    public ResponseEntity<Map<String, Object>> getChatSessionMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long chatHistoryId) {

        Long userId = user.getUser().getUserId();

        // 1. 내 소유인지 확인 (예외 발생 시 403 방지용)
        chatService.validateChatHistoryOwnership(userId, chatHistoryId);

        // 2. FastAPI에 세션 조회 요청
        Map<String, Object> sessionData = chatService.getSessionMessagesFromPython(chatHistoryId);

        return ResponseEntity.ok(sessionData);
    }
}