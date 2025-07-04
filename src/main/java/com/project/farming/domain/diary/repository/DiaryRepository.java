package com.project.farming.domain.diary.repository;

import com.project.farming.domain.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Diary> findDistinctByUserIdAndDiaryPlantsPlantPlantIdInOrderByCreatedAtDesc(Long userId, List<Long> plantIds);
}
