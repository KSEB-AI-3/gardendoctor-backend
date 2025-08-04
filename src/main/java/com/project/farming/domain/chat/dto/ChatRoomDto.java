package com.project.farming.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private Long chatId; // Spring DB의 채팅방 ID
    private Long pythonSessionId; // Python 서버의 세션 ID
}