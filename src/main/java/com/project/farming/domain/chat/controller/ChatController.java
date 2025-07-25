package com.project.farming.domain.chat.controller;

import com.project.farming.domain.chat.service.ChatService;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}