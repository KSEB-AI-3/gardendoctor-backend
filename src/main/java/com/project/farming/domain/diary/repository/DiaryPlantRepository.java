package com.project.farming.domain.diary.repository;

import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.entity.DiaryPlant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryPlantRepository extends JpaRepository<DiaryPlant, Long> {
    void deleteAllByDiary(Diary diary);
}
