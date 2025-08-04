package com.project.farming.domain.chat.repository;

import com.project.farming.domain.chat.entity.Chat;
import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * 사용자를 기준으로 채팅방 정보를 조회합니다.
     * 사용자와 채팅방은 1:1 관계이므로 Optional<Chat>을 반환합니다.
     * @param user 조회할 사용자 엔티티
     * @return Optional<Chat>
     */
    Optional<Chat> findByUser(User user);

    List<Chat> findByUser_UserId(Long userId);
}
