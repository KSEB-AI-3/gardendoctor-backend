package com.project.farming.domain.plant.service;

import com.project.farming.domain.plant.dto.PlantRequest;
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
@RequiredArgsConstructor
@Service
public class PlantService {

    private final PlantRepository plantRepository;

    @Transactional
    public PlantResponse savePlant(PlantRequest request) {
        if (plantRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 식물입니다: " + request.getName());
        }
        Plant newPlant = Plant.builder()
                .name(request.getName())
                .englishName(request.getEnglishName())
                .species(request.getSpecies())
                .season(request.getSeason())
                .imageUrl(request.getImageUrl())
                .build();
        Plant savedPlant = plantRepository.save(newPlant);
        return toPlantResponseBuilder(savedPlant)
                .plantId(savedPlant.getPlantId())
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
    public PlantResponse updatePlant(Long plantId, PlantRequest request) {
        Plant plant = findPlantById(plantId);
        plant.updatePlant(request.getName(), request.getEnglishName(),
                request.getSpecies(), request.getSeason(), request.getImageUrl());
        Plant updatedPlant = plantRepository.save(plant);
        return toPlantResponseBuilder(updatedPlant).build();
    }

    @Transactional
    public void deletePlant(Long plantId) {
        Plant plant = findPlantById(plantId);
        plantRepository.delete(plant);
    }

    private PlantResponse.PlantResponseBuilder toPlantResponseBuilder(Plant plant) {
        return PlantResponse.builder()
                .name(plant.getName())
                .englishName(plant.getEnglishName())
                .species(plant.getSpecies())
                .season(plant.getSeason())
                .imageUrl(plant.getImageUrl());
    }

    private Plant findPlantById(Long plantId) {
        return plantRepository.findById(plantId)
                .orElseThrow(() -> new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId));
    }
}
