package com.project.farming.domain.plant.service;

import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.global.exception.PlantNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PlantService {

    private final PlantRepository plantRepository;

    /**
     * 전체 식물 목록 조회(이름순)
     *
     * @return 각 식물 정보의 Response DTO 리스트
     */
    public List<PlantResponse> findAllPlants() {
        List<Plant> foundPlants = plantRepository.findAllByOrderByPlantNameAsc();
        if (foundPlants.isEmpty()) {
            log.info("등록된 식물이 없습니다.");
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
