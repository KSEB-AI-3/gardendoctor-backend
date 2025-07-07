package com.project.farming.domain.diary.repository;

import com.project.farming.domain.diary.entity.DiaryUserPlant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryUserPlantRepository extends JpaRepository<DiaryUserPlant, Long> {
}