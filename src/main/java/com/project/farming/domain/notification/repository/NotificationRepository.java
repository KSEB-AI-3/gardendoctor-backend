package com.project.farming.domain.notification.repository;

import com.project.farming.domain.notification.entity.Notification;
import com.project.farming.domain.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 사용자의 알림을 최신순으로 조회 (페이징 적용)
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 특정 사용자 ID를 가진 알림을 최신순으로 조회 (서비스의 getUserNotifications에서 사용)
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 특정 사용자의 읽지 않은 알림 개수 조회
    long countByUserAndIsReadFalse(User user);

    // 특정 사용자의 모든 알림 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user.userId = :userId") // JPQL 쿼리도 User 엔티티의 필드 이름을 'userId'로 변경
    void deleteByUserId(@Param("userId") Long userId);
}