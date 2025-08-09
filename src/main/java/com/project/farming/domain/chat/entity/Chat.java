package com.project.farming.domain.chat.entity;

import com.project.farming.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    // 한 명의 사용자는 여러개의 채팅방을 가집니다 (1:N 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Python FastAPI 서버에서 관리하는 세션의 ID
    @Column(name = "python_session_id", nullable = false)
    private Long pythonSessionId;
}
