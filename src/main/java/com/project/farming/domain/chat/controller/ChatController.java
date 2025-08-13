package com.project.farming.domain.chat.controller;

import com.project.farming.domain.chat.dto.ChatRequestDto;
import com.project.farming.domain.chat.dto.ChatResponseDto;
import com.project.farming.domain.chat.dto.ChatRoomDto;
import com.project.farming.domain.chat.dto.PythonChatDto;
import com.project.farming.domain.chat.entity.Chat;
import com.project.farming.domain.chat.repository.ChatRepository;
import com.project.farming.domain.chat.service.ChatService;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat API", description = "작물 챗봇 질문 및 답변 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRepository chatRepository; // 세션 ID 조회를 위해 추가

    @Operation(summary = "챗봇 질문 전송", description = "사용자의 질문을 Python 챗봇 서버로 보내고 응답을 반환합니다. 대화 맥락이 유지됩니다.")
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(
            @RequestBody ChatRequestDto requestBody,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = (userDetails != null ? userDetails.getUser() : null);

        ChatResponseDto responseDto = chatService.askPythonAgent(
                user,
                requestBody.getChatId(), // 클라이언트가 보낸 chatId
                requestBody.getQuery()
        );

        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "특정 대화의 답변만 조회", description = "FastAPI에서 특정 세션의 모든 메시지 중 챗봇 답변(assistant)만 조회합니다.")
    @GetMapping("/history/messages")
    public ResponseEntity<List<PythonChatDto.PythonChatMessage>> getChatSessionMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("chatId") Long chatId) { // ✨ @RequestParam에 이름 명시

        User user = userDetails.getUser();
        Long userId = user.getUserId();

        chatService.validateChatHistoryOwnership(userId, chatId);

        Long pythonSessionId = chatRepository.findById(chatId)
                .map(Chat::getPythonSessionId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 정보를 찾을 수 없습니다."));

        List<PythonChatDto.PythonChatMessage> sessionMessages = chatService.getSessionMessagesFromPython(pythonSessionId);

        return ResponseEntity.ok(sessionMessages);
    }
    // ✨ [수정된 부분]
    @Operation(summary = "특정 채팅방의 전체 응답 내용 조회", description = "특정 채팅방의 전체 응답 내용 조회 (user, assistant)을 모두 조회합니다.")
    @GetMapping("/history/messages/all")
    public ResponseEntity<List<PythonChatDto.PythonChatMessage>> getChatSessionMessagesAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("chatId") Long chatId) {

        Long userId = userDetails.getUser().getUserId();

        chatService.validateChatHistoryOwnership(userId, chatId);

        Long pythonSessionId = chatRepository.findById(chatId)
                .map(Chat::getPythonSessionId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 정보를 찾을 수 없습니다."));

        // 새로 추가한 전체 메시지 조회 메소드 호출
        List<PythonChatDto.PythonChatMessage> sessionMessages = chatService.getAllSessionMessagesFromPython(pythonSessionId);

        return ResponseEntity.ok(sessionMessages);
    }

    @Operation(summary = "최신 챗봇 대화방 목록 조회", description = "사용자의 최신 대화 목록을 조회합니다. Spring의 chatId와 Python의 sessionId를 함께 반환합니다.")
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatRoomDto>> getChatRoomList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        List<ChatRoomDto> chatRooms = chatService.getChatRoomList(user);

        return ResponseEntity.ok(chatRooms);
    }

    /**
     * ✨ [새로 추가된 메소드]
     */
    @Operation(summary = "챗봇 대화방 삭제", description = "특정 챗봇 대화방을 삭제합니다.")
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatId) {

        Long userId = userDetails.getUser().getUserId();
        chatService.deleteChatRoom(userId, chatId);

        return ResponseEntity.ok().build();
    }
}