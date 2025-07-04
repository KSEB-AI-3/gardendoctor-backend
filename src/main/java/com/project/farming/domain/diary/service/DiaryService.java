package com.project.farming.domain.diary.service;

import com.project.farming.domain.diary.dto.DiaryRequestDto;
import com.project.farming.domain.diary.dto.DiaryResponseDto;
import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.entity.DiaryPlant;
import com.project.farming.domain.diary.repository.DiaryPlantRepository;
import com.project.farming.domain.diary.repository.DiaryRepository;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryPlantRepository diaryPlantRepository;
    private final UserRepository userRepository;
    private final PlantRepository plantRepository;

    @Transactional
    public Long createDiary(Long userId, DiaryRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Diary diary = Diary.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .watered(request.isWatered())
                .pruned(request.isPruned())
                .fertilized(request.isFertilized())
                .build();

        diaryRepository.save(diary);

        attachPlantsToDiary(diary, request.getPlantIds());

        return diary.getDiaryId();
    }

    @Transactional
    public void updateDiary(Long userId, Long diaryId, DiaryRequestDto request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일지입니다."));

        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 일지에 대한 권한이 없습니다.");
        }

        diary.updateDiary(
                request.getTitle(),
                request.getContent(),
                request.getImageUrl(),
                request.isWatered(),
                request.isPruned(),
                request.isFertilized()
        );

        diaryPlantRepository.deleteAllByDiary(diary);
        diary.clearDiaryPlants();

        attachPlantsToDiary(diary, request.getPlantIds());
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일지입니다."));

        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 일지에 대한 권한이 없습니다.");
        }

        diaryRepository.delete(diary);
    }

    @Transactional
    public DiaryResponseDto getDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일지입니다."));

        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 일지에 대한 권한이 없습니다.");
        }

        return convertToDto(diary);
    }

    @Transactional
    public List<DiaryResponseDto> getDiaryList(Long userId, List<Long> plantIds) {
        List<Diary> diaries = (plantIds == null || plantIds.isEmpty()) ?
                diaryRepository.findAllByUserIdOrderByCreatedAtDesc(userId) :
                diaryRepository.findDistinctByUserIdAndDiaryPlantsPlantPlantIdInOrderByCreatedAtDesc(userId, plantIds);

        return diaries.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private void attachPlantsToDiary(Diary diary, List<Long> plantIds) {
        if (plantIds != null && !plantIds.isEmpty()) {
            List<Plant> plants = plantRepository.findAllById(plantIds);

            if (plants.size() != plantIds.size()) {
                throw new IllegalArgumentException("유효하지 않은 식물이 포함되어 있습니다.");
            }

            plants.forEach(plant -> {
                DiaryPlant diaryPlant = DiaryPlant.builder()
                        .diary(diary)
                        .plant(plant)
                        .build();

                diaryPlantRepository.save(diaryPlant);
                diary.addDiaryPlant(diaryPlant);
            });
        }
    }

    private DiaryResponseDto convertToDto(Diary diary) {
        List<DiaryResponseDto.PlantSummary> plantSummaries = diary.getDiaryPlants().stream()
                .map(dp -> DiaryResponseDto.PlantSummary.builder()
                        .plantId(dp.getPlant().getPlantId())
                        .name(dp.getPlant().getName())
                        .imageUrl(dp.getPlant().getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return DiaryResponseDto.builder()
                .diaryId(diary.getDiaryId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .imageUrl(diary.getImageUrl())
                .watered(diary.isWatered())
                .pruned(diary.isPruned())
                .fertilized(diary.isFertilized())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .plants(plantSummaries)
                .build();
    }
}
