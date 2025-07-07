package com.project.farming.domain.diary.service;

import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.entity.DiaryUserPlant;
import com.project.farming.domain.diary.repository.DiaryRepository;
import com.project.farming.domain.diary.repository.DiaryUserPlantRepository;
import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.plant.repository.UserPlantRepository;
import com.project.farming.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션, 쓰기 메서드에만 @Transactional
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryUserPlantRepository diaryUserPlantRepository;
    private final UserPlantRepository userPlantRepository;

    /**
     * 일지 생성
     *
     * @param user 현재 로그인한 사용자
     * @param title 일지 제목
     * @param content 일지 내용
     * @param imageUrl 이미지 URL
     * @param watered 물주기 여부
     * @param pruned 가지치기 여부
     * @param fertilized 영양제 주기 여부
     * @param selectedUserPlantIds 일지에 연결할 UserPlant ID 목록 (선택 사항)
     * @return 생성된 Diary 엔티티
     */
    @Transactional
    public Diary createDiary(User user, String title, String content, String imageUrl,
                             boolean watered, boolean pruned, boolean fertilized,
                             List<Long> selectedUserPlantIds) {
        // 새로운 Diary 엔티티 생성 및 기본 정보 설정
        Diary diary = Diary.builder()
                .user(user)
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .watered(watered)
                .pruned(pruned)
                .fertilized(fertilized)
                .build();
        diaryRepository.save(diary); // Diary 먼저 저장

        // 선택된 UserPlant들이 있다면 연결
        if (selectedUserPlantIds != null && !selectedUserPlantIds.isEmpty()) {
            List<UserPlant> userPlants = userPlantRepository.findAllById(selectedUserPlantIds);

            // 요청된 모든 UserPlant ID가 실제로 존재하는지, 그리고 현재 사용자의 것인지 검증
            if (userPlants.size() != selectedUserPlantIds.size()) {
                throw new IllegalArgumentException("Some selected UserPlants were not found.");
            }
            for (UserPlant userPlant : userPlants) {
                if (!userPlant.getUser().getUserId().equals(user.getUserId())) { // ID 비교로 변경
                    throw new IllegalArgumentException("User does not own one of the selected UserPlants.");
                }
            }

            // DiaryUserPlant 연결 엔티티 생성 및 Diary에 추가
            for (UserPlant userPlant : userPlants) {
                DiaryUserPlant diaryUserPlant = DiaryUserPlant.builder()
                        .diary(diary)
                        .userPlant(userPlant)
                        .build();
                diary.addDiaryUserPlant(diaryUserPlant); // Diary 엔티티 내부에서 양방향 관계 설정
            }
            diaryUserPlantRepository.saveAll(diary.getDiaryUserPlants()); // 연결 엔티티 저장
        }
        return diary;
    }

    /**
     * 일지 수정
     *
     * @param diaryId 수정할 일지 ID
     * @param user 현재 로그인한 사용자
     * @param title 일지 제목
     * @param content 일지 내용
     * @param imageUrl 이미지 URL
     * @param watered 물주기 여부
     * @param pruned 가지치기 여부
     * @param fertilized 영양제 주기 여부
     * @param newUserPlantIds 새로 연결할 UserPlant ID 목록 (선택 사항)
     * @return 수정된 Diary 엔티티
     */
    @Transactional
    public Diary updateDiary(Long diaryId, User user, String title, String content, String imageUrl,
                             boolean watered, boolean pruned, boolean fertilized,
                             List<Long> newUserPlantIds) {
        // 일지 조회 및 권한 확인
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NoSuchElementException("Diary not found with ID: " + diaryId));

        if (!diary.getUser().getUserId().equals(user.getUserId())) { // ID 비교로 변경
            throw new IllegalArgumentException("User does not have permission to update this diary.");
        }

        // Diary 기본 정보 업데이트
        diary.updateDiary(title, content, imageUrl, watered, pruned, fertilized);

        // 기존 DiaryUserPlant 연결 모두 제거
        // orphanRemoval=true 설정으로 DB에서도 해당 연결 데이터가 삭제됨
        diary.clearDiaryUserPlants();

        // 새로운 UserPlant 연결 추가
        if (newUserPlantIds != null && !newUserPlantIds.isEmpty()) {
            List<UserPlant> newUserPlants = userPlantRepository.findAllById(newUserPlantIds);

            // 요청된 모든 UserPlant ID가 실제로 존재하는지, 그리고 현재 사용자의 것인지 검증
            if (newUserPlants.size() != newUserPlantIds.size()) {
                throw new IllegalArgumentException("Some selected UserPlants were not found for update.");
            }
            for (UserPlant userPlant : newUserPlants) {
                if (!userPlant.getUser().getUserId().equals(user.getUserId())) { // ID 비교로 변경
                    throw new IllegalArgumentException("User does not own one of the selected UserPlants for update.");
                }
            }

            // 새로운 DiaryUserPlant 연결 엔티티 생성 및 Diary에 추가
            for (UserPlant userPlant : newUserPlants) {
                DiaryUserPlant diaryUserPlant = DiaryUserPlant.builder()
                        .diary(diary)
                        .userPlant(userPlant)
                        .build();
                diary.addDiaryUserPlant(diaryUserPlant);
            }
            diaryUserPlantRepository.saveAll(diary.getDiaryUserPlants()); // 새로운 연결 엔티티 저장
        }

        return diary; // 변경된 Diary 반환 (영속성 컨텍스트에 의해 자동 업데이트됨)
    }

    /**
     * 일지 삭제
     *
     * @param diaryId 삭제할 일지 ID
     * @param user 현재 로그인한 사용자
     */
    @Transactional
    public void deleteDiary(Long diaryId, User user) {
        // 일지 조회 및 권한 확인
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NoSuchElementException("Diary not found with ID: " + diaryId));

        if (!diary.getUser().getUserId().equals(user.getUserId())) { // ID 비교로 변경
            throw new IllegalArgumentException("User does not have permission to delete this diary.");
        }
        diaryRepository.delete(diary); // 일지 삭제 (CascadeType.ALL과 orphanRemoval=true 설정으로 연결된 DiaryUserPlant도 삭제됨)
    }

    /**
     * 특정 일지 조회
     *
     * @param diaryId 조회할 일지 ID
     * @param user 현재 로그인한 사용자
     * @return 조회된 Diary 엔티티
     */
    public Diary getDiaryById(Long diaryId, User user) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NoSuchElementException("Diary not found with ID: " + diaryId));
        if (!diary.getUser().getUserId().equals(user.getUserId())) { // ID 비교로 변경
            throw new IllegalArgumentException("User does not have permission to view this diary.");
        }
        return diary;
    }

    /**
     * 특정 사용자의 모든 일지 조회 (캘린더 기본 뷰 - 최신순)
     *
     * @param user 현재 로그인한 사용자
     * @return 해당 사용자의 모든 Diary 목록
     */
    public List<Diary> getAllDiariesByUser(User user) {
        return diaryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 특정 사용자의 특정 기간 동안의 일지 조회 (캘린더 날짜별 정렬)
     *
     * @param user 현재 로그인한 사용자
     * @param startDate 조회 시작 날짜/시간
     * @param endDate 조회 종료 날짜/시간
     * @return 해당 기간 동안의 Diary 목록
     */
    public List<Diary> getDiariesByUserAndDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return diaryRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, startDate, endDate);
    }

    /**
     * 특정 사용자가 등록한 특정 UserPlant(사용자 식물)별 일지 조회 (닉네임 기반 태그 검색)
     *
     * @param user 현재 로그인한 사용자
     * @param userPlantId 검색할 UserPlant ID
     * @return 해당 UserPlant에 연결된 Diary 목록
     */
    public List<Diary> getDiariesByUserAndUserPlant(User user, Long userPlantId) {
        // UserPlant가 현재 사용자의 것인지 확인하며 조회
        UserPlant userPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new NoSuchElementException("UserPlant not found with ID: " + userPlantId + " for this user."));

        return diaryRepository.findByUserAndUserPlant(user, userPlant);
    }

    /**
     * 특정 사용자가 등록한 여러 UserPlant 중 하나라도 포함된 일지 조회 (다중 태그 검색)
     *
     * @param user 현재 로그인한 사용자
     * @param userPlantIds 검색할 UserPlant ID 목록
     * @return 해당 UserPlant 중 하나라도 연결된 Diary 목록
     */
    public List<Diary> getDiariesByUserAndUserPlants(User user, List<Long> userPlantIds) {
        List<UserPlant> userPlants = userPlantRepository.findAllById(userPlantIds);

        // 요청된 모든 UserPlant ID가 실제로 존재하는지 검증
        if (userPlants.size() != userPlantIds.size()) {
            throw new IllegalArgumentException("Some UserPlants not found from provided IDs.");
        }
        // UserPlant들이 현재 사용자의 것인지 검증
        for (UserPlant up : userPlants) {
            if (!up.getUser().getUserId().equals(user.getUserId())) { // ID 비교로 변경
                throw new IllegalArgumentException("User does not own one of the selected UserPlants.");
            }
        }

        return diaryRepository.findByUserAndUserPlantsIn(user, userPlants);
    }
}