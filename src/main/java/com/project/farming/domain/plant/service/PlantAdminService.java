package com.project.farming.domain.plant.service;

import com.project.farming.domain.plant.dto.PlantAdminRequest;
import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.domain.userplant.repository.UserPlantRepository;
import com.project.farming.global.exception.ImageFileNotFoundException;
import com.project.farming.global.exception.PlantNotFoundException;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PlantAdminService {

    private final PlantRepository plantRepository;
    private final ImageFileService imageFileService;
    private final ImageFileRepository imageFileRepository;
    private final UserPlantRepository userPlantRepository;

    /**
     * 새로운 식물 정보 등록
     *
     * @param request 등록할 식물 정보
     * @param file 업로드할 식물 이미지 파일 (선택적)
     */
    @Transactional
    public void savePlant(PlantAdminRequest request, MultipartFile file) {
        if (plantRepository.existsByPlantName(request.getPlantName())) {
            log.error("이미 존재하는 식물입니다: {}", request.getPlantName());
            throw new IllegalArgumentException("이미 존재하는 식물입니다: " + request.getPlantName());
        }
        ImageFile defaultImageFile = getDefaultImageFile();
        Plant newPlant = Plant.builder()
                .plantName(getOrDefault(request.getPlantName()))
                .plantEnglishName(getOrDefault(request.getPlantEnglishName()))
                .species(getOrDefault(request.getSpecies()))
                .season(getOrDefault(request.getSeason()))
                .plantImageFile(defaultImageFile)
                .build();
        Plant savedPlant = plantRepository.save(newPlant);
        Long plantId = savedPlant.getPlantId();

        if (file != null && !file.isEmpty()) {
            // 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.uploadImage(file, ImageDomainType.PLANT, plantId);
            savedPlant.updatePlantImage(imageFile);
        }
    }

    /**
     * 전체 식물 목록 조회(ID 순)
     *
     * @return 각 식물 정보의 Response DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PlantResponse> findAllPlants() {
        List<Plant> foundPlants = plantRepository.findAllByOrderByPlantIdAsc();
        if (foundPlants.isEmpty()) {
            log.info("등록된 식물이 없습니다.");
        }
        return foundPlants.stream()
                .map(plant -> toPlantResponseBuilder(plant).build())
                .collect(Collectors.toList());
    }

    /**
     * 식물 목록 검색(ID 순)
     * - 식물의 한글 이름 또는 영어 이름으로 검색(통합)
     *
     * @param keyword 검색어(식물 이름)
     * @return 검색된 식물 정보의 Response DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PlantResponse> findPlantsByKeyword(String keyword) {
        List<Plant> foundPlants = plantRepository.findByPlantNameContainingOrderByPlantIdAsc("%"+keyword+"%");
        return foundPlants.stream()
                .map(plant -> toPlantResponseBuilder(plant).build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 식물 정보 수정
     *
     * @param plantId 수정할 식물 정보의 ID
     * @param request 새로 저장할 식물 정보
     * @param newFile 새로 업로드할 식물 이미지 파일 (선택적)
     */
    @Transactional
    public void updatePlant(Long plantId, PlantAdminRequest request, MultipartFile newFile) {
        Plant plant = findPlantById(plantId);
        if (newFile != null && !newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.updateImage(
                    plant.getPlantImageFile().getImageFileId(), // 기존 이미지 파일
                    newFile, ImageDomainType.PLANT, plantId);
            plant.updatePlantImage(imageFile);
        }
        plant.updatePlantInfo(getOrDefault(request.getPlantName()),
                getOrDefault(request.getPlantEnglishName()),
                getOrDefault(request.getSpecies()), getOrDefault(request.getSeason()));
        plantRepository.save(plant);
    }

    /**
     * 특정 식물 정보 삭제
     * - 삭제할 식물과 매핑된 userPlant가 있다면
     *   해당 userPlant의 식물을 '기타'로 변경
     *
     * @param plantId 삭제할 식물 정보의 ID
     */
    @Transactional
    public void deletePlant(Long plantId) {
        Plant plant = findPlantById(plantId);
        if ("기타".equals(plant.getPlantName())) {
            log.error("기본 식물 정보는 삭제할 수 없습니다.");
            throw new RuntimeException("기본 식물 정보는 삭제할 수 없습니다.");
        }
        Plant otherPlant = plantRepository.getOtherPlant("기타")
                .orElseThrow(() -> {
                    log.error("DB에 '기타' 항목이 존재하지 않습니다.");
                    return new PlantNotFoundException("DB에 '기타' 항목이 존재하지 않습니다.");
                });
        int updatedCount = userPlantRepository.reassignPlant(otherPlant, plant);
        if (updatedCount == 0) log.info("해당 식물({})과 매핑된 사용자 식물이 없습니다.", plantId);
        else log.info(
                "해당 식물({})과 매핑된 사용자 식물 {}개의 식물 정보가 '기타'로 수정되었습니다.", plantId, updatedCount);
        plantRepository.delete(plant);
        imageFileService.deleteImage(plant.getPlantImageFile().getImageFileId()); // 기존 이미지 파일
    }

    /**
     * 식물 기본 이미지 반환
     *
     * @return 식물 기본 이미지
     */
    private ImageFile getDefaultImageFile() {
        return imageFileRepository.findByS3Key(DefaultImages.DEFAULT_PLANT_IMAGE)
                .orElseThrow(() -> {
                    log.error("기본 식물 이미지가 존재하지 않습니다.");
                    return new ImageFileNotFoundException("기본 식물 이미지가 존재하지 않습니다.");
                });
    }

    /**
     * 기본값 설정(String)
     *
     * @param val 확인할 값
     * @return 기본값 또는 request 값
     */
    private String getOrDefault(String val) {
        return val == null || val.isBlank() || val.equals("-") ? "N/A" : val;
    }

    /**
     * Response DTO로 변환
     *
     * @param plant Response DTO로 변환할 식물 정보 엔티티
     * @return 식물 정보 Response DTO
     */
    private PlantResponse.PlantResponseBuilder toPlantResponseBuilder(Plant plant) {
        return PlantResponse.builder()
                .plantId(plant.getPlantId())
                .plantName(plant.getPlantName())
                .plantEnglishName(plant.getPlantEnglishName())
                .species(plant.getSpecies())
                .season(plant.getSeason())
                .plantImageUrl(plant.getPlantImageFile().getImageUrl());
    }

    /**
     * ID로 식물 정보 조회
     *
     * @param plantId 조회할 식물 정보의 ID
     * @return 조회한 식물 정보
     */
    private Plant findPlantById(Long plantId) {
        return plantRepository.findById(plantId)
                .orElseThrow(() -> {
                    log.error("해당 식물이 존재하지 않습니다: {}", plantId);
                    return new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId);
                });
    }
}
