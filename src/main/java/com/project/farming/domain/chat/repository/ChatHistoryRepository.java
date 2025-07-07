package com.project.farming.domain.chat.repository;

import com.project.farming.domain.chat.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByUser_UserId(Long userId);
}
