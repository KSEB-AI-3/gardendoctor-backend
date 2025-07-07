package com.project.farming.domain.subscription.repository;

import com.project.farming.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // 변경: Subscription 엔티티의 'user' 필드 안의 'userId'를 기준으로 찾음
    List<Subscription> findByUser_UserId(Long userId);
}