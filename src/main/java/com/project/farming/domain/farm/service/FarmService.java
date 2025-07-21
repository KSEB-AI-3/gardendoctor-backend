package com.project.farming.domain.farm.service;

import com.project.farming.domain.farm.dto.FarmRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import com.project.farming.global.exception.FarmNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class FarmService {

    private final FarmRepository farmRepository;

    @Transactional
    public FarmResponse saveFarm(FarmRequest request) {
        if (farmRepository.existsByGardenUniqueId(request.getGardenUniqueId())) {
            throw new IllegalArgumentException("이미 존재하는 텃밭입니다: " + request.getGardenUniqueId());
        }
        Farm newFarm = Farm.builder()
                .gardenUniqueId(request.getGardenUniqueId())
                .operator(request.getOperator())
                .name(request.getName())
                .roadNameAddress(request.getRoadNameAddress())
                .lotNumberAddress(request.getLotNumberAddress())
                .facilities(request.getFacilities())
                .available(request.getAvailable())
                .contact(request.getContact())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .build();
        Farm savedFarm = farmRepository.save(newFarm);
        return toFarmResponseBuilder(savedFarm, true)
                .farmId(savedFarm.getFarmId())
                .build();
    }
    
    public List<FarmResponse> findAllFarms() {
        List<Farm> foundFarms = farmRepository.findAllByOrderByGardenUniqueIdAsc();
        if (foundFarms.isEmpty()) {
            throw new FarmNotFoundException("등록된 텃밭이 없습니다.");
        }
        return foundFarms.stream()
                .map(farm -> toFarmResponseBuilder(farm, false).build())
                .collect(Collectors.toList());
    }

    public List<FarmResponse> findFarmsByKeyword(String searchType, String keyword) {
        List<Farm> foundFarms = switch (searchType) {
            case "name" -> farmRepository.findByNameContainingOrderByGardenUniqueIdAsc(keyword);
            case "address" -> farmRepository.findByAddressContainingOrderByGardenUniqueIdAsc("%" + keyword + "%");
            default -> throw new IllegalArgumentException("지원하지 않는 검색 조건입니다: " + searchType);
        };
        return foundFarms.stream()
                .map(farm -> toFarmResponseBuilder(farm, false).build())
                .collect(Collectors.toList());
    }

    public FarmResponse findFarm(Long farmId) {
        Farm foundFarm = findFarmById(farmId);
        return toFarmResponseBuilder(foundFarm, true).build();
    }

    @Transactional
    public FarmResponse updateFarm(Long farmId, FarmRequest request) {
        Farm farm = findFarmById(farmId);
        farm.updateFarmInfo(request.getGardenUniqueId(), request.getOperator(), request.getName(),
                request.getRoadNameAddress(), request.getLotNumberAddress(), request.getFacilities(),
                request.getAvailable(), request.getContact(), request.getLatitude(), request.getLongitude(),
                request.getImageUrl());
        Farm updatedFarm = farmRepository.save(farm);
        return toFarmResponseBuilder(updatedFarm, true).build();
    }

    @Transactional
    public void deleteFarm(Long farmId) {
        Farm farm = findFarmById(farmId);
        farmRepository.delete(farm);
    }

    public List<FarmResponse> findFarmsByCurrentLocation (Double latitude, Double longitude, Double radius) {
        log.info("현재 위치: {}, {} / 반경: {}", latitude, longitude, radius);
        List<Farm> foundFarms = farmRepository.findFarmsWithinRadius(
                latitude, longitude, radius * 1000); // 미터 단위로 계산
        return foundFarms.stream()
                .map(farm -> toFarmResponseBuilder(farm, true).build())
                .collect(Collectors.toList());
    }

    private FarmResponse.FarmResponseBuilder toFarmResponseBuilder(Farm farm, boolean includeDetails) {
        FarmResponse.FarmResponseBuilder builder = FarmResponse.builder()
                .gardenUniqueId(farm.getGardenUniqueId())
                .operator(farm.getOperator())
                .name(farm.getName())
                .lotNumberAddress(farm.getLotNumberAddress())
                .updatedAt(farm.getUpdatedAt())
                .imageUrl(farm.getImageUrl());
        if (includeDetails) {
            builder.roadNameAddress(farm.getRoadNameAddress())
                    .facilities(farm.getFacilities())
                    .available(farm.getAvailable())
                    .contact(farm.getContact())
                    .latitude(farm.getLatitude())
                    .longitude(farm.getLongitude())
                    .createdAt(farm.getCreatedAt());
        }
        return builder;
    }

    private Farm findFarmById(Long farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new FarmNotFoundException("해당 텃밭이 존재하지 않습니다: " + farmId));
    }
}
