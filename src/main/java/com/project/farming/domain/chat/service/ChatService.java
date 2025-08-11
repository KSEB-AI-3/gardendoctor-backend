package com.project.farming.domain.chat.service;

import com.project.farming.domain.chat.dto.ChatResponseDto;
import com.project.farming.domain.chat.dto.ChatRoomDto;
import com.project.farming.domain.chat.dto.PythonChatDto;
import com.project.farming.domain.chat.dto.PythonSessionDto;
import com.project.farming.domain.chat.entity.Chat;
import com.project.farming.domain.chat.repository.ChatRepository;
import com.project.farming.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    @Qualifier("pythonWebClient")
    private final WebClient pythonWebClient;

    /**
     * Python 챗봇 에이전트에게 질문하고 답변을 받습니다.
     * 항상 새로운 세션을 생성합니다.
     *
     * @param user      인증된 사용자 또는 null
     * @param question  클라이언트에서 받은 질문
     * @param chatId    세션 id 조회용 PK
     * @return 에이전트가 생성한 답변
     */
    @Transactional
    public ChatResponseDto askPythonAgent(User user, Long chatId, String question) {
        Long pythonSessionId = null;

        // --- 요청 처리: 클라이언트가 보낸 chatId(세션 ID)가 있는 경우 ---
        if (chatId != null) {
            Chat existingChat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

            // (보안) 본인 채팅방이 맞는지 확인
            if (user != null && !existingChat.getUser().getUserId().equals(user.getUserId())) {
                throw new SecurityException("해당 채팅방에 접근할 권한이 없습니다.");
            }
            // FastAPI에 보낼 pythonSessionId를 DB에서 조회
            pythonSessionId = existingChat.getPythonSessionId();
        }

        // FastAPI 서버에 요청 (pythonSessionId가 null이면 FastAPI가 새 세션을 만듦)
        PythonChatDto.PythonChatRequest request = new PythonChatDto.PythonChatRequest(pythonSessionId, question);

        PythonChatDto.PythonChatResponse responseFromPython = pythonWebClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PythonChatDto.PythonChatResponse.class)
                .block();

        String answer = responseFromPython.getMessages().stream()
                .filter(msg -> "assistant".equalsIgnoreCase(msg.getRole()))
                .reduce((first, second) -> second)
                .map(PythonChatDto.PythonChatMessage::getQuery)
                .orElse("답변을 찾을 수 없습니다.");

        // --- 응답 처리: 첫 채팅이었던 경우 ---
        if (chatId == null && user != null) {
            Chat newChat = Chat.builder()
                    .user(user)
                    .pythonSessionId(responseFromPython.getId()) // FastAPI가 새로 만든 세션ID 저장
                    .build();
            Chat savedChat = chatRepository.save(newChat);
            chatId = savedChat.getChatId(); // 새로 생성된 DB의 chatId를 확정
        }

        // 최종적으로 클라이언트에게 보낼 응답 생성
        return ChatResponseDto.builder()
                .answer(answer)
                .chatId(chatId) // 기존 chatId 또는 새로 생성된 chatId
                .build();
    }

    public void validateChatHistoryOwnership(Long userId, Long chatId) {
        chatRepository.findById(chatId)
                .filter(chat -> chat.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅 기록을 조회할 수 없습니다."));
    }

    public List<PythonChatDto.PythonChatMessage> getSessionMessagesFromPython(Long pythonSessionId) {
        PythonChatDto.PythonChatResponse response = pythonWebClient.get()
                .uri("/api/chat/sessions/{id}", pythonSessionId)
                .retrieve()
                .bodyToMono(PythonChatDto.PythonChatResponse.class)
                .block();

        if (response == null || response.getMessages() == null) {
            return List.of();
        }

        return response.getMessages().stream()
                .filter(msg -> "assistant".equalsIgnoreCase(msg.getRole()))
                .collect(Collectors.toList());
    }

    private List<PythonSessionDto> getSessionListFromPython() {
        return pythonWebClient.get()
                .uri("/api/chat/sessions")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PythonSessionDto>>() {})
                .block();
    }

    public List<ChatRoomDto> getChatRoomList(User user) {
        Long userId = user.getUserId();

        // 유저의 모든 Chat 엔티티 가져오기
        Map<Long, Long> springChatToPythonSessionMap = chatRepository.findByUser_UserId(userId).stream()
                .collect(Collectors.toMap(Chat::getPythonSessionId, Chat::getChatId));

        List<PythonSessionDto> pythonSessions = getSessionListFromPython();

        return pythonSessions.stream()
                .filter(pythonSession -> springChatToPythonSessionMap.containsKey(pythonSession.getId()))
                .map(pythonSession -> ChatRoomDto.builder()
                        .chatId(springChatToPythonSessionMap.get(pythonSession.getId()))
                        .pythonSessionId(pythonSession.getId())
                        .query(pythonSession.getQuery())
                        .createdAt(pythonSession.getCreatedAt())
                        .updatedAt(pythonSession.getUpdatedAt())
                        .messageCount(pythonSession.getMessageCount())
                        .build())
                .collect(Collectors.toList());
    }
}
