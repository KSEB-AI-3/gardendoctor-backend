package com.project.farming.domain.analysis.repository;

import com.project.farming.domain.analysis.entity.PhotoAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoAnalysisRepository extends JpaRepository<PhotoAnalysis, Long> {
    List<PhotoAnalysis> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
