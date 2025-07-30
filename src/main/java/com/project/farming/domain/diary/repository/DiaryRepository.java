package com.project.farming.domain.diary.repository;

import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.userplant.entity.UserPlant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    // 특정 사용자의 모든 일지를 최신 생성일 기준으로 내림차순 정렬하여 조회
    List<Diary> findByUserOrderByCreatedAtDesc(User user);

    // 특정 사용자의 특정 기간 동안의 일지를 생성일 기준으로 오름차순 정렬하여 조회
    List<Diary> findByUserAndCreatedAtBetweenOrderByCreatedAtAsc(User user, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // 특정 사용자의 특정 UserPlant에 연결된 일지를 최신 생성일 기준으로 내림차순 정렬하여 조회
    @Query("SELECT d FROM Diary d JOIN d.diaryUserPlants dup WHERE d.user = :user AND dup.userPlant = :userPlant ORDER BY d.createdAt DESC")
    List<Diary> findByUserAndUserPlant(@Param("user") User user, @Param("userPlant") UserPlant userPlant);

    // 특정 사용자의 여러 UserPlant 중 하나라도 연결된 일지를 최신 생성일 기준으로 내림차순 정렬하여 조회
    @Query("SELECT DISTINCT d FROM Diary d JOIN d.diaryUserPlants dup WHERE d.user = :user AND dup.userPlant IN :userPlants ORDER BY d.createdAt DESC")
    List<Diary> findByUserAndUserPlantsIn(@Param("user") User user, @Param("userPlants") List<UserPlant> userPlants);
}