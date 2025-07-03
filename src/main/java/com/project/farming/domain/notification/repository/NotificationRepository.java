package com.project.farming.domain.notification.repository;

import com.project.farming.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 사용자에 대한 알림 목록 조회 (최신순)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 사용자에 대해 읽지 않은 알림만 조회
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // 사용자 ID 기반 삭제 (회원 탈퇴 시)
    void deleteByUserId(Long userId);
}
