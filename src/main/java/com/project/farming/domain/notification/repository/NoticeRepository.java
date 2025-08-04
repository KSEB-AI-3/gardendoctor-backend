package com.project.farming.domain.notification.repository;

import com.project.farming.domain.notification.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    boolean existsByTitleAndContent(String title, String content);
    List<Notice> findAllByOrderByNoticeIdAsc();
    List<Notice> findByTitleContainingOrderByNoticeIdAsc(String keyword);
    List<Notice> findByContentContainingOrderByNoticeIdAsc(String keyword);
}
