package com.project.farming.domain.diary.service;

import com.project.farming.domain.diary.dto.DiaryResponse;
import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.entity.DiaryUserPlant;
import com.project.farming.domain.diary.repository.DiaryRepository;
import com.project.farming.domain.diary.repository.DiaryUserPlantRepository;
import com.project.farming.domain.userplant.entity.UserPlant;
import com.project.farming.domain.userplant.repository.UserPlantRepository;
import com.project.farming.domain.userplant.service.UserPlantDailyStatusRedisService;
import com.project.farming.domain.user.entity.User;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryUserPlantRepository diaryUserPlantRepository;
    private final UserPlantRepository userPlantRepository;
    private final UserPlantDailyStatusRedisService userPlantDailyStatusRedisService;
    private final ImageFileService imageFileService;

    /**
     * 일지 생성
     */
    @Transactional
    public Diary createDiary(User user, String title, String content, LocalDate diaryDate,
                             MultipartFile imageFile, boolean watered, boolean pruned, boolean fertilized,
                             List<Long> selectedUserPlantIds) {
        // 새로운 Diary 엔티티 생성 및 기본 정보 설정
        Diary diary = Diary.builder()
                .user(user)
                .title(title)
                .content(content)
                .diaryDate(diaryDate) // ✨ diaryDate 설정
                .watered(watered)
                .pruned(pruned)
                .fertilized(fertilized)
                .build();
        diaryRepository.save(diary);

        if (imageFile != null && !imageFile.isEmpty()) {
            ImageFile uploadedImage = imageFileService.uploadImage(imageFile, ImageDomainType.DIARY, diary.getDiaryId());
            diary.setDiaryImage(uploadedImage); // Diary 엔티티에 ImageFile 연결
        }

        if (selectedUserPlantIds != null && !selectedUserPlantIds.isEmpty()) {
            List<UserPlant> userPlants = userPlantRepository.findAllById(selectedUserPlantIds);
            if (userPlants.size() != selectedUserPlantIds.size()) {
                throw new IllegalArgumentException("일부 선택된 식물을 찾을 수 없습니다.");
            }
            for (UserPlant userPlant : userPlants) {
                if (!userPlant.getUser().getUserId().equals(user.getUserId())) {
                    throw new IllegalArgumentException("본인의 식물이 아닌 식물이 선택되었습니다.");
                }
                // ✨ 이 부분을 추가합니다.
                userPlant.updateUserPlantStatus(watered, pruned, fertilized);
                DiaryUserPlant diaryUserPlant = DiaryUserPlant.builder()
                        .diary(diary)
                        .userPlant(userPlant)
                        .build();
                diary.addDiaryUserPlant(diaryUserPlant);
            }
            diaryUserPlantRepository.saveAll(diary.getDiaryUserPlants());
        }

        for (DiaryUserPlant diaryUserPlant : diary.getDiaryUserPlants()) {
            Long userPlantId = diaryUserPlant.getUserPlant().getUserPlantId();
            userPlantDailyStatusRedisService.updateStatusOnDiaryWrite(userPlantId, watered, pruned, fertilized);
        }

        return diary;
    }

    /**
     * 일지 수정
     */
    @Transactional
    public Diary updateDiary(Long diaryId, User user, String title, String content, LocalDate diaryDate,
                             MultipartFile newImageFile, boolean deleteExistingImage,
                             boolean watered, boolean pruned, boolean fertilized,
                             List<Long> newUserPlantIds) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 일지를 찾을 수 없습니다: " + diaryId));

        if (!diary.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 일지에 대한 수정 권한이 없습니다.");
        }

        if (newImageFile != null && !newImageFile.isEmpty()) {
            if (diary.getDiaryImageFile() != null) {
                imageFileService.deleteImage(diary.getDiaryImageFile().getImageFileId());
            }
            ImageFile uploadedImage = imageFileService.uploadImage(newImageFile, ImageDomainType.DIARY, diary.getDiaryId());
            diary.setDiaryImage(uploadedImage);
        } else if (deleteExistingImage && diary.getDiaryImageFile() != null) {
            imageFileService.deleteImage(diary.getDiaryImageFile().getImageFileId());
            diary.setDiaryImage(null);
        }

        diary.updateDiary(title, content, diaryDate, diary.getDiaryImageFile(), watered, pruned, fertilized); // ✨ diaryDate 전달

        diary.clearDiaryUserPlants();
        diaryUserPlantRepository.deleteAll(diaryUserPlantRepository.findByDiary(diary));

        if (newUserPlantIds != null && !newUserPlantIds.isEmpty()) {
            List<UserPlant> newUserPlants = userPlantRepository.findAllById(newUserPlantIds);

            if (newUserPlants.size() != newUserPlantIds.size()) {
                throw new IllegalArgumentException("일부 선택된 식물을 찾을 수 없습니다 (수정).");
            }
            for (UserPlant userPlant : newUserPlants) {
                if (!userPlant.getUser().getUserId().equals(user.getUserId())) {
                    throw new IllegalArgumentException("본인의 식물이 아닌 식물이 선택되었습니다 (수정).");
                }
            }
            for (UserPlant userPlant : newUserPlants) {
                DiaryUserPlant diaryUserPlant = DiaryUserPlant.builder()
                        .diary(diary)
                        .userPlant(userPlant)
                        .build();
                diary.addDiaryUserPlant(diaryUserPlant);
            }
            diaryUserPlantRepository.saveAll(diary.getDiaryUserPlants());
        }

        for (DiaryUserPlant diaryUserPlant : diary.getDiaryUserPlants()) {
            Long userPlantId = diaryUserPlant.getUserPlant().getUserPlantId();
            userPlantDailyStatusRedisService.updateStatusOnDiaryWrite(userPlantId, watered, pruned, fertilized);
        }

        return diary;
    }

    /**
     * 일지 삭제
     */
    @Transactional
    public void deleteDiary(Long diaryId, User user) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 일지를 찾을 수 없습니다: " + diaryId));
        if (!diary.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 일지에 대한 삭제 권한이 없습니다.");
        }
        if (diary.getDiaryImageFile() != null) {
            imageFileService.deleteImage(diary.getDiaryImageFile().getImageFileId());
        }
        diaryRepository.delete(diary);
    }

    /**
     * 특정 일지 조회
     */
    public DiaryResponse getDiaryById(Long diaryId, User user) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 일지를 찾을 수 없습니다: " + diaryId));
        if (!diary.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 일지에 대한 조회 권한이 없습니다.");
        }
        return new DiaryResponse(diary);
    }

    /**
     * 특정 사용자의 모든 일지 조회 (캘린더 기본 뷰 - 최신순)
     */
    public List<DiaryResponse> getAllDiariesByUser(User user) {
        List<Diary> diaries = diaryRepository.findByUserOrderByCreatedAtDesc(user);
        return diaries.stream().map(DiaryResponse::new).collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 특정 기간 동안의 일지 조회 (캘린더 날짜별 정렬)
     */
    public List<DiaryResponse> getDiariesByUserAndDateRange(User user, LocalDate startDate, LocalDate endDate) { // ✨ LocalDate로 변경
        // `diaryDate` 필드를 기준으로 조회하도록 변경해야 합니다.
        // 이를 위해 `DiaryRepository`에 새로운 메서드를 추가해야 합니다.
        // 예: `findByUserAndDiaryDateBetween(User user, LocalDate startDate, LocalDate endDate)`
        // 현재 코드에서는 `findByUserAndCreatedAtBetweenOrderByCreatedAtAsc`를 사용하고 있으므로,
        // 이 부분을 repository에 맞는 메서드로 변경해야 합니다.
        List<Diary> diaries = diaryRepository.findByUserAndDiaryDateBetweenOrderByDiaryDateAsc(user, startDate, endDate);
        return diaries.stream().map(DiaryResponse::new).collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 등록한 특정 UserPlant(사용자 식물)별 일지 조회 (닉네임 기반 태그 검색)
     */
    public List<DiaryResponse> getDiariesByUserAndUserPlant(User user, Long userPlantId) {
        UserPlant userPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new NoSuchElementException("사용자에 대한 해당 식물을 찾을 수 없습니다: " + userPlantId));
        List<Diary> diaries = diaryRepository.findByUserAndUserPlant(user, userPlant);
        return diaries.stream().map(DiaryResponse::new).collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 등록한 여러 UserPlant 중 하나라도 포함된 일지 조회 (다중 태그 검색)
     */
    public List<DiaryResponse> getDiariesByUserAndUserPlants(User user, List<Long> userPlantIds) {
        List<UserPlant> userPlants = userPlantRepository.findAllById(userPlantIds);
        if (userPlants.size() != userPlantIds.size()) {
            throw new IllegalArgumentException("일부 선택된 식물을 찾을 수 없습니다.");
        }
        for (UserPlant up : userPlants) {
            if (!up.getUser().getUserId().equals(user.getUserId())) {
                throw new IllegalArgumentException("본인의 식물이 아닌 식물이 포함되어 있습니다.");
            }
        }
        List<Diary> diaries = diaryRepository.findByUserAndUserPlantsIn(user, userPlants);
        return diaries.stream().map(DiaryResponse::new).collect(Collectors.toList());
    }
}
