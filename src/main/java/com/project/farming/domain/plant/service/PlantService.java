package com.project.farming.domain.plant.service;

import com.project.farming.domain.plant.dto.PlantRequest;
import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.global.exception.ImageFileNotFoundException;
import com.project.farming.global.exception.PlantNotFoundException;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import com.project.farming.global.image.service.ImageFileService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final ImageFileService imageFileService;
    private final ImageFileRepository imageFileRepository;

    /**
     * 새로운 식물 정보 등록
     *
     * @param request 등록할 식물 정보
     * @param file 업로드할 식물 이미지 파일 (선택적)
     * @return 저장된 식물 정보의 Response DTO
     */
    @Transactional
    public PlantResponse savePlant(PlantRequest request, MultipartFile file) {
        if (plantRepository.existsByPlantName(request.getPlantName())) {
            throw new IllegalArgumentException("이미 존재하는 식물입니다: " + request.getPlantName());
        }
        ImageFile defaultImageFile = imageFileRepository.findByS3Key(DefaultImages.DEFAULT_PLANT_IMAGE)
                .orElseThrow(() -> new ImageFileNotFoundException("기본 식물 이미지가 존재하지 않습니다."));
        Plant newPlant = Plant.builder()
                .plantName(request.getPlantName())
                .plantEnglishName(request.getPlantEnglishName())
                .species(request.getSpecies())
                .season(request.getSeason())
                .plantImageFile(defaultImageFile)
                .build();
        Plant savedPlant = plantRepository.save(newPlant);
        Long plantId = savedPlant.getPlantId();

        if (!file.isEmpty()) {
            // 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.uploadImage(file, ImageDomainType.PLANT, plantId);
            savedPlant.updatePlantImage(imageFile);
        }
        return toPlantResponseBuilder(savedPlant)
                .plantId(plantId)
                .build();
    }

    /**
     * 전체 식물 목록 조회(이름순)
     *
     * @return 각 식물 정보의 Response DTO 리스트
     */
    public List<PlantResponse> findAllPlants() {
        List<Plant> foundPlants = plantRepository.findAllByOrderByPlantNameAsc();
        if (foundPlants.isEmpty()) {
            throw new PlantNotFoundException("등록된 식물이 없습니다.");
        }
        return foundPlants.stream()
                .map(plant -> toPlantResponseBuilder(plant).build())
                .collect(Collectors.toList());
    }

    /**
     * 식물 목록 검색(이름순)
     * - 식물의 한글 이름 또는 영어 이름으로 검색(통합)
     *
     * @param keyword 검색어(식물 이름)
     * @return 검색된 식물 정보의 Response DTO 리스트
     */
    public List<PlantResponse> findPlantsByKeyword(String keyword) {
        List<Plant> foundPlants = plantRepository.findByPlantNameContainingOrderByPlantNameAsc("%"+keyword+"%");
        return foundPlants.stream()
                .map(plant -> toPlantResponseBuilder(plant).build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 식물 정보 조회
     *
     * @param plantId 조회할 식물 정보의 ID
     * @return 해당 식물 정보의 Response DTO
     */
    public PlantResponse findPlant(Long plantId) {
        Plant foundPlant = findPlantById(plantId);
        return toPlantResponseBuilder(foundPlant).build();
    }

    /**
     * 특정 식물 정보 수정
     *
     * @param plantId 수정할 식물 정보의 ID
     * @param request 새로 저장할 식물 정보
     * @param newFile 새로 업로드할 식물 이미지 파일 (선택적)
     * @return 수정된 식물 정보의 Response DTO
     */
    @Transactional
    public PlantResponse updatePlant(Long plantId, PlantRequest request, MultipartFile newFile) {
        Plant plant = findPlantById(plantId);
        if (!newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.updateImage(
                    plant.getPlantImageFile().getImageFileId(), // 기존 이미지 파일
                    newFile, ImageDomainType.PLANT, plantId);
            plant.updatePlantImage(imageFile);
        }
        plant.updatePlantInfo(request.getPlantName(), request.getPlantEnglishName(),
                request.getSpecies(), request.getSeason());
        Plant updatedPlant = plantRepository.save(plant);
        return toPlantResponseBuilder(updatedPlant).build();
    }

    /**
     * 특정 식물 정보 삭제
     *
     * @param plantId 삭제할 식물 정보의 ID
     */
    @Transactional
    public void deletePlant(Long plantId) {
        Plant plant = findPlantById(plantId);
        plantRepository.delete(plant);
        imageFileService.deleteImage(plant.getPlantImageFile().getImageFileId()); // 기존 이미지 파일
    }

    /**
     * Response DTO로 변환
     *
     * @param plant Response DTO로 변환할 식물 정보 엔티티
     * @return 식물 정보 Response DTO
     */
    private PlantResponse.PlantResponseBuilder toPlantResponseBuilder(Plant plant) {
        return PlantResponse.builder()
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
                .orElseThrow(() -> new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId));
    }

    /**
     * PlantDataInitializer에서 사용
     *
     * @param plantList 저장할 초기 식물 정보 목록
     */
    @Transactional
    public void savePlants(List<Plant> plantList) {
        plantRepository.saveAll(plantList);
    }
}
