package com.project.farming.domain.chat.service;

import com.project.farming.domain.chat.dto.ChatRoomDto;
import com.project.farming.domain.chat.dto.PythonChatDto;
import com.project.farming.domain.chat.dto.PythonSessionDto;
import com.project.farming.domain.chat.entity.Chat;
import com.project.farming.domain.chat.repository.ChatRepository; // 가상의 Chat 리포지토리
import com.project.farming.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    // ChatHistoryRepository 대신 ChatRoomRepository를 사용합니다.
    // 이 리포지토리는 사용자와 Python 세션 ID를 매핑합니다.
    private final ChatRepository chatRepository;

    @Qualifier("pythonWebClient")
    private final WebClient pythonWebClient;

    /**
     * Python 챗봇 에이전트에게 질문하고 답변을 받습니다.
     * 세션을 관리하여 대화의 맥락을 유지합니다.
     *
     * @param user     인증된 사용자 또는 null
     * @param question 클라이언트에서 받은 질문
     * @return 에이전트가 생성한 답변
     */
    @Transactional
    public String askPythonAgent(User user, String question) {
        Long pythonSessionId = null;

        // 1. 인증된 사용자라면, 기존 대화방(세션)이 있는지 확인합니다.
        if (user != null) {
            Optional<Chat> chat = chatRepository.findByUser(user);
            if (chat.isPresent()) {
                pythonSessionId = chat.get().getPythonSessionId();
            }
        }
        // 비인증 사용자는 항상 새로운 세션으로 대화합니다 (pythonSessionId = null).

        // 2. Python FastAPI 서버에 보낼 요청 데이터를 준비합니다.
        PythonChatDto.PythonChatRequest request = new PythonChatDto.PythonChatRequest(pythonSessionId, question);

        // 3. Python FastAPI 서버 호출
        PythonChatDto.PythonChatResponse response = pythonWebClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PythonChatDto.PythonChatResponse.class)
                .block();

        if (response == null || response.getMessages() == null || response.getMessages().isEmpty()) {
            return "죄송해요, 응답을 받지 못했습니다.";
        }

        // 4. (인증된 사용자) 새로운 대화였다면, 반환된 세션 ID를 DB에 저장합니다.
        if (user != null && pythonSessionId == null) {
            Chat newChat = Chat.builder()
                    .user(user)
                    .pythonSessionId(response.getId())
                    .build();
            chatRepository.save(newChat);
        }

        // 5. 응답에서 AI의 마지막 답변을 찾아서 반환합니다.
        return response.getMessages().stream()
                .filter(msg -> "assistant".equalsIgnoreCase(msg.getRole()))
                .reduce((first, second) -> second) // 마지막 요소 찾기
                .map(PythonChatDto.PythonChatMessage::getQuery)
                .orElse("죄송해요, 답변을 찾을 수 없습니다.");
    }

    public void validateChatHistoryOwnership(Long userId, Long chatId) {
        // Chat 엔티티 기준으로 소유권 검증 로직 수정
        chatRepository.findById(chatId)
                .filter(chat -> chat.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅 기록을 조회할 수 없습니다."));
    }

    /**
     * Python 챗봇 서버에서 특정 세션의 메시지들을 조회하고, assistant 메시지만 반환합니다.
     */
    public List<PythonChatDto.PythonChatMessage> getSessionMessagesFromPython(Long pythonSessionId) {
        // 1. FastAPI의 /api/chat/sessions/{session_id} 엔드포인트 호출
        PythonChatDto.PythonChatResponse response = pythonWebClient.get()
                .uri("/api/chat/sessions/{id}", pythonSessionId)
                .retrieve()
                .bodyToMono(PythonChatDto.PythonChatResponse.class)
                .block();

        // 2. 응답이 유효한지 확인하고 메시지 목록을 가져옵니다.
        if (response == null || response.getMessages() == null) {
            return List.of(); // 빈 리스트 반환
        }

        // 3. 메시지 목록에서 'assistant' 역할의 메시지만 필터링하여 반환합니다.
        return response.getMessages().stream()
                .filter(msg -> "assistant".equalsIgnoreCase(msg.getRole()))
                .collect(Collectors.toList());
    }

    /**
     * Python 챗봇 서버에서 사용자의 모든 세션 목록을 조회합니다.
     * @return 세션 ID를 포함한 세션 목록
     */
    private List<PythonSessionDto> getSessionListFromPython() {
        return pythonWebClient.get()
                .uri("/api/chat/sessions")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PythonSessionDto>>() {})
                .block();
    }

    /**
     * 사용자의 챗봇 대화방 목록을 조회합니다.
     * Spring DB의 chatId와 Python 서버의 sessionId를 결합하여 DTO로 반환합니다.
     * @param user 인증된 사용자
     * @return 사용자의 모든 채팅방 목록 (ChatRoomDto 리스트)
     */
    public List<ChatRoomDto> getChatRoomList(User user) {
        Long userId = user.getUserId();

        // 1. Spring DB에서 사용자의 채팅방 정보를 가져옵니다.
        // 현재는 OneToOne 관계이므로, 하나의 Chat 객체만 존재합니다.
        Map<Long, Long> springChatToPythonSessionMap = chatRepository.findByUser_UserId(userId).stream()
                .collect(Collectors.toMap(Chat::getPythonSessionId, Chat::getChatId));

        // 2. Python FastAPI 서버의 세션 목록을 가져옵니다.
        List<PythonSessionDto> pythonSessions = getSessionListFromPython();

        // 3. Spring DB에 있는 세션만 필터링하여 ChatRoomDto로 매핑합니다.
        return pythonSessions.stream()
                .filter(pythonSession -> springChatToPythonSessionMap.containsKey(pythonSession.getId()))
                .map(pythonSession -> ChatRoomDto.builder()
                        .chatId(springChatToPythonSessionMap.get(pythonSession.getId()))
                        .pythonSessionId(pythonSession.getId())
                        // 필요하다면 다른 필드를 추가할 수 있습니다.
                        // .createdAt(pythonSession.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}