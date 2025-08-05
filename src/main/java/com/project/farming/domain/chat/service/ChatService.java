package com.project.farming.domain.chat.service;

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
     * @param user     인증된 사용자 또는 null
     * @param question 클라이언트에서 받은 질문
     * @return 에이전트가 생성한 답변
     */
    @Transactional
    public String askPythonAgent(User user, String question) {
        // 1. Python 서버로 보낼 요청 생성 (세션 ID 없음 = 새 세션)
        PythonChatDto.PythonChatRequest request = new PythonChatDto.PythonChatRequest(null, question);

        // 2. Python FastAPI 서버 호출
        PythonChatDto.PythonChatResponse response = pythonWebClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PythonChatDto.PythonChatResponse.class)
                .block();

        if (response == null || response.getMessages() == null || response.getMessages().isEmpty()) {
            return "죄송해요, 응답을 받지 못했습니다.";
        }

        // 3. 사용자 인증된 경우, 세션 정보 DB에 저장
        if (user != null) {
            Chat newChat = Chat.builder()
                    .user(user)
                    .pythonSessionId(response.getId())
                    .build();
            chatRepository.save(newChat);
        }

        // 4. 마지막 assistant 메시지를 찾아서 반환
        return response.getMessages().stream()
                .filter(msg -> "assistant".equalsIgnoreCase(msg.getRole()))
                .reduce((first, second) -> second)
                .map(PythonChatDto.PythonChatMessage::getQuery)
                .orElse("죄송해요, 답변을 찾을 수 없습니다.");
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
