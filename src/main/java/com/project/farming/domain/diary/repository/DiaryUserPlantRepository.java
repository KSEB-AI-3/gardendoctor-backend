package com.project.farming.domain.diary.repository;

import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.entity.DiaryUserPlant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryUserPlantRepository extends JpaRepository<DiaryUserPlant, Long> {
    List<DiaryUserPlant> findByDiary(Diary diary);
}