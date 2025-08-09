// src/main/farming/domain/chat/dto/ChatRequestDto.java
package com.project.farming.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequestDto {
    private Long chatId;
    private String query;
}