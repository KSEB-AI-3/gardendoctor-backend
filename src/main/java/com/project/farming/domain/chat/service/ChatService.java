package com.project.farming.domain.chat.service;

import com.project.farming.domain.chat.entity.ChatHistory;
import com.project.farming.domain.chat.repository.ChatHistoryRepository;
import com.project.farming.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatHistoryRepository historyRepo;

    @Qualifier("pythonWebClient")
    private final WebClient pythonWebClient;

    /**
     * @param user    인증된 사용자 또는 null
     * @param question 클라이언트에서 받은 질문
     * @return 에이전트가 생성한 답변
     */
    public String askPythonAgent(User user, String question) {
        // 1) 인증된 사용자라면 질문 저장
        if (user != null) {
            historyRepo.save(
                    ChatHistory.builder()
                            .user(user)
                            .message(question)
                            .role("USER")
                            .build()
            );
        }

        // 2) Python FastAPI 서버 호출 ({"query": ...} 형태)
        Map<String, String> resp = pythonWebClient.post()
                .uri("/chat")
                .bodyValue(Map.of("query", question))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .block();

        String answer = (resp != null && resp.get("answer") != null)
                ? resp.get("answer")
                : "죄송해요, 응답이 없습니다.";

        // 3) 인증된 사용자라면 답변 저장
        if (user != null) {
            historyRepo.save(
                    ChatHistory.builder()
                            .user(user)
                            .message(answer)
                            .role("BOT")
                            .build()
            );
        }

        return answer;
    }
}
