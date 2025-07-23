package com.project.farming.domain.notification.repository;

import com.project.farming.domain.notification.entity.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Transactional
    // JPQL 쿼리도 User 엔티티의 필드 이름을 'userId'로 변경
    @Query("DELETE FROM Notification n WHERE n.user.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}