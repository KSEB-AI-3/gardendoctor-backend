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

    @Transactional
    public PlantResponse savePlant(PlantRequest request, MultipartFile file) {
        if (plantRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 식물입니다: " + request.getName());
        }
        ImageFile defaultImageFile = imageFileRepository.findByS3Key(DefaultImages.DEFAULT_PLANT_IMAGE)
                .orElseThrow(() -> new ImageFileNotFoundException("기본 식물 이미지가 존재하지 않습니다."));
        Plant newPlant = Plant.builder()
                .name(request.getName())
                .englishName(request.getEnglishName())
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

    public List<PlantResponse> findAllPlants() {
        List<Plant> foundPlants = plantRepository.findAllByOrderByNameAsc();
        if (foundPlants.isEmpty()) {
            throw new PlantNotFoundException("등록된 식물이 없습니다.");
        }
        return foundPlants.stream()
                .map(plant -> toPlantResponseBuilder(plant).build())
                .collect(Collectors.toList());
    }

    public List<PlantResponse> findPlantsByKeyword(String keyword) {
        List<Plant> foundPlants = plantRepository.findByNameContainingOrderByNameAsc(keyword);
        return foundPlants.stream()
                .map(plant -> toPlantResponseBuilder(plant).build())
                .collect(Collectors.toList());
    }

    public PlantResponse findPlant(Long plantId) {
        Plant foundPlant = findPlantById(plantId);
        return toPlantResponseBuilder(foundPlant).build();
    }

    @Transactional
    public PlantResponse updatePlant(Long plantId, PlantRequest request, MultipartFile newFile) {
        Plant plant = findPlantById(plantId);
        ImageFile oldImageFile = plant.getPlantImageFile(); // 기존 이미지 파일
        ImageFile imageFile = oldImageFile;
        if (!newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            imageFile = imageFileService.updateImage(oldImageFile.getImageFileId(),
                    newFile, ImageDomainType.PLANT, plantId);
        }
        plant.updatePlant(request.getName(), request.getEnglishName(),
                request.getSpecies(), request.getSeason());
        plant.updatePlantImage(imageFile);
        Plant updatedPlant = plantRepository.save(plant);
        return toPlantResponseBuilder(updatedPlant).build();
    }

    @Transactional
    public void deletePlant(Long plantId) {
        Plant plant = findPlantById(plantId);
        ImageFile oldImageFile = plant.getPlantImageFile(); // 기존 이미지 파일
        plantRepository.delete(plant);
        imageFileService.deleteImage(oldImageFile.getImageFileId());
    }

    private PlantResponse.PlantResponseBuilder toPlantResponseBuilder(Plant plant) {
        return PlantResponse.builder()
                .name(plant.getName())
                .englishName(plant.getEnglishName())
                .species(plant.getSpecies())
                .season(plant.getSeason())
                .imageUrl(plant.getPlantImageFile().getImageUrl());
    }

    private Plant findPlantById(Long plantId) {
        return plantRepository.findById(plantId)
                .orElseThrow(() -> new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId));
    }
}
