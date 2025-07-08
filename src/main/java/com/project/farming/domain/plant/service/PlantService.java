package com.project.farming.domain.plant.service;

import com.project.farming.domain.plant.dto.PlantRequestDto;
import com.project.farming.domain.plant.dto.PlantResponseDto;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.global.exception.PlantNotFoundException;
import jakarta.transaction.Transactional;
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
    public PlantResponseDto savePlant(PlantRequestDto request) {
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
        return PlantResponseDto.builder()
                .message("해당 식물이 성공적으로 등록되었습니다.")
                .name(savedPlant.getName())
                .build();
    }

    public List<PlantResponseDto> findAllPlants() {
        List<Plant> foundPlants = plantRepository.findAllByOrderByNameAsc();
        if (foundPlants.isEmpty()) {
            throw new PlantNotFoundException("등록된 식물이 없습니다.");
        }
        return foundPlants.stream()
                .map(plant -> PlantResponseDto.builder()
                        .plantId(plant.getPlantId())
                        .name(plant.getName())
                        .englishName(plant.getEnglishName())
                        .species(plant.getSpecies())
                        .season(plant.getSeason())
                        .imageUrl(plant.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    public PlantResponseDto findPlant(Long plantId) {
        Plant foundPlant = plantRepository.findById(plantId)
                .orElseThrow(() -> new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId));
        return PlantResponseDto.builder()
                .plantId(foundPlant.getPlantId())
                .name(foundPlant.getName())
                .englishName(foundPlant.getEnglishName())
                .species(foundPlant.getSpecies())
                .season(foundPlant.getSeason())
                .imageUrl(foundPlant.getImageUrl())
                .build();
    }

    @Transactional
    public PlantResponseDto updatePlant(Long plantId, PlantRequestDto request) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId));
        plant.updatePlant(request.getName(), request.getEnglishName(),
                request.getSpecies(), request.getSeason(), request.getImageUrl());
        Plant updatedPlant = plantRepository.save(plant);
        return PlantResponseDto.builder()
                .message("해당 식물 정보가 성공적으로 수정되었습니다.")
                .name(updatedPlant.getName())
                .build();
    }

    @Transactional
    public void deletePlant(Long plantId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new PlantNotFoundException("해당 식물이 존재하지 않습니다: " + plantId));
        plantRepository.delete(plant);
    }
}
